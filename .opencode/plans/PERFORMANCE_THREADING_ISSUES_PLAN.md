# Critical Performance & Threading Issues - Analysis & Plan

## Issue 1: Synchronous Asset Unpacking in VoiceToTextModule

**Problem Location:**
- File: `VoiceToTextModule.kt:26-37`
- Method: `provideVoskModel()`
- Calls: `AssetsUtil.unpackAssetsFolder()` synchronously

**Current Flow:**
```
Hilt @Provides → provideVoskModel() → AssetsUtil.unpackAssetsFolder() [BLOCKING]
```
- Runs on Hilt's initialization thread (could be main thread)
- Model folder ~50-100MB, causes significant blocking
- AssetsUtil.kt:24-60 shows recursive file copy - I/O intensive

**Why It's a Risk:**
- ANR (Application Not Responding) on first app launch or after clean install
- Blocks dependency graph initialization
- User sees frozen screen

**2026 Best Practice:**
- Background unpacking with coroutines
- UI handles "model loading" state
- Cache check before initialization

---

### Solution: Move to Background with CoroutineScope

**Option A: Lazy initialization with coroutine (RECOMMENDED)**

```kotlin
@Provides
@Singleton
fun provideVoskModel(@ApplicationContext context: Context): Model? {
    // Still synchronous return - model should already be unpacked
    // Pre-unpack during app startup, not here
    return try {
        val modelPath = File(context.filesDir, VOSK_MODEL_PATH).absolutePath
        if (File(modelPath, "am/final.mdl").exists()) {
            Model(modelPath)
        } else null
    } catch (e: Exception) { null }
}
```

**Option B: Unpack during app startup (Application class)**

```kotlin
// In LevelingUpApp.kt - add to onCreate():
CoroutineScope(Dispatchers.IO).launch {
    AssetsUtil.unpackAssetsFolder(context, VOSK_MODEL_PATH)
}
```

**Option C: Use a dedicated Worker**

```kotlin
// Create VoskModelWorker extending CoroutineWorker
// Schedule during first launch
```

**Implementation Order:**
1. Add CoroutineScope import to LevelingUpApp
2. Move unpacking to app startup (async)
3. Keep VoiceToTextModule as lazy load (returns null until ready)
4. Add loading state in UI layer

---

## Issue 2: Blocking Auth Interceptor

**Problem Location:**
- File: `AuthInterceptor.kt:36-60`
- Method: `getValidToken()`
- Uses: `Tasks.await(task, 5, TimeUnit.SECONDS)` - **blocking call**

**Current Flow:**
```
OkHttp Dispatcher Thread → getValidToken() → Tasks.await() [BLOCKING]
```
- Blocks OkHttp thread pool thread
- Under high concurrency → thread pool exhaustion
- New requests queue behind blocked ones

**Why It's a Risk:**
- Thread pool exhaustion under load
- Request timeouts
- System-wide backpressure

**2026 Best Practice (OkHttp):**
- Use OkHttp's `Authenticator` for 401 retries (non-blocking)
- Pre-cache tokens in ViewModel/Repository layer
- Never use `runBlocking` or `Tasks.await` in interceptors

---

### Solution: Use Authenticator or Pre-cache

**Option A: Use OkHttp Authenticator (RECOMMENDED)**

```kotlin
class TokenAuthenticator : Authenticator {
    private var cachedToken: String? = null
    private var tokenExpiry: Long = 0

    override fun authenticate(route: Route?, response: Response): Request? {
        // Only retry once
        if (response.request.header("Authorization") != null) {
            return null
        }

        val token = getValidToken() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun getValidToken(): String? {
        // Non-blocking - return cached or fetch async
    }
}
```

**Option B: Pre-cache in Repository (already exist)**

Most repositories pre-fetch tokens. Verify:
- Check if PlayerRepositoryImpl pre-caches tokens
- If yes, interceptors should work without blocking

**Option C: Lazy token fetch in interceptor**

```kotlin
private fun getValidToken(user: FirebaseUser): String {
    // Check cache first
    cachedToken?.takeIf { System.currentTimeMillis() < tokenExpiry }
        ?: run {
            // Fetch async, return cached immediately
            fetchTokenAsync()
        }
}
```

**Implementation Order:**
1. Remove `Tasks.await()` from AuthInterceptor
2. Implement `Authenticator` interface
3. Configure OkHttpClient with .authenticator()
4. Keep token caching logic (already exists)

---

## Issue 3: Manual Thread Management in LevelingUpApp

**Problem Location:**
- File: `LevelingUpApp.kt:33-41`
- Code:
```kotlin
Thread {
    scheduleWeeklySync()
    scheduleMidnightPenalty()
    scheduleNightlySync()
}.apply {
    name = "WorkManagerInit"
    isDaemon = true
    start()
}
```

**Why It's a Risk:**
- Legacy Java-style (2026: use coroutines)
- No cancellation handling
- No structured concurrency
- Daemon thread may be killed before work completes

**2026 Best Practice:**
- Use `CoroutineScope(Dispatchers.IO).launch`
- Or use `Handler(Looper.getMainLooper())` for main thread
- WorkManager can be scheduled from main thread

---

### Solution: Use CoroutineScope

```kotlin
// Inside onCreate():
CoroutineScope(Dispatchers.IO).launch {
    scheduleWeeklySync()
    scheduleMidnightPenalty()
    scheduleNightlySync()
}
// Or execute directly on main thread (WorkManager.init is fast):
scheduleWeeklySync()
scheduleMidnightPenalty()
scheduleNightlySync()
```

**Note:** WorkManager scheduling is synchronous but fast. The real issue is:
- These calls should be on main thread for safety
- Or use structured concurrency with CoroutineScope

**Implementation:**
```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// In LevelingUpApp.kt:
override fun onCreate() {
    super.onCreate()
    // ... existing code ...

    CoroutineScope(Dispatchers.IO).launch {
        scheduleWeeklySync()
        scheduleMidnightPenalty()
        scheduleNightlySync()
    }
}
```

---

## Summary

| Issue | Problem | Solution | Priority |
|-------|---------|----------|----------|
| 1. VoiceToTextModule | Blocking I/O in @Provides | Move unpacking to app startup | HIGH |
| 2. AuthInterceptor | Blocking Tasks.await | Use Authenticator or pre-cache | HIGH |
| 3. LevelingUpApp | Thread {}.start() | CoroutineScope.launch | MEDIUM |

---

## Implementation Order

1. **HIGH:** Fix VoiceToTextModule blocking (ANR risk)
2. **HIGH:** Fix AuthInterceptor blocking (thread exhaustion)
3. **MEDIUM:** Modernize LevelingUpApp thread handling

---

## Files to Modify

1. `LevelingUpApp.kt` - add CoroutineScope, move unpacking
2. `VoiceToTextModule.kt` - keep lazy load (model check)
3. `AuthInterceptor.kt` - remove blocking, add Authenticator
4. Add new `TokenAuthenticator.kt` (if Option A)