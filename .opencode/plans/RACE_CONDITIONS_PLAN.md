# Race Conditions - Calendar Selection Fix

## Problem Analysis

### Current Flow
```
User taps date → CalendarViewmodel._selectedDate updated
    ↓
DashboardScreen: LaunchedEffect(selectedDate) { getSessionForDate(date) }
    ↓
DashboardViewModel.getSessionForDate() launches NEW coroutine
    ↓
Multiple coroutines race → _todaySession updated out of order
```

### Race Condition Details

**File:** `DashboardViewModel.kt:131-136`

```kotlin
fun getSessionForDate(localDate: LocalDate) {
    viewModelScope.launch(Dispatchers.IO) {
        Timber.tag(TAG).d("getSessionForDate() called for: $localDate")
        loadSessionForDate(localDate)
    }
}
```

**Problem:**
- Every call to `getSessionForDate()` creates a NEW coroutine
- If user rapidly taps dates (tap-tap-tap), multiple coroutines run concurrently
- Each coroutine writes to `_todaySession` → UI shows wrong data
- The LAST coroutine to complete wins, not necessarily the last date tapped

**Race Timeline Example:**
```
T=0ms: User taps Jan 15 → Coroutine A starts (query Jan 15)
T=50ms: User taps Jan 16 → Coroutine B starts (query Jan 16)
T=100ms: User taps Jan 17 → Coroutine C starts (query Jan 17)
T=200ms: Coroutine C completes → UI shows Jan 17
T=300ms: Coroutine B completes → UI shows Jan 16 ← WRONG (older result)
T=500ms: Coroutine A completes → UI shows Jan 15 ← WRONG (oldest result)
```

---

## Solution: Using flatMapLatest

### Concept
Instead of launching new coroutines for each date selection, use `flatMapLatest` to:
1. Cancel any previous coroutine when a new date is selected
2. Only the most recent date's query result updates the UI

### Implementation

**New Architecture:**
```kotlin
// Replace current getSessionForDate() approach

private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())

val todaySession: StateFlow<Resource<TrainingSession?>> = _selectedDate
    .flatMapLatest { date ->
        flow {
            emit(Resource.Loading())
            exerciseRepository.getSessionForDate(
                Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
            ).collect { emit(it) }
        }
    }
    .flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
```

### Alternative: Using switchMap with Channel

If flatMapLatest isn't available, use a different pattern:

```kotlin
// In DashboardViewModel:
private val _sessionQuery = MutableSharedFlow<LocalDate>(extraBufferCapacity = 1)

val todaySession: StateFlow<Resource<TrainingSession?>> = _sessionQuery
    .map { date ->
        // Load and return result
    }
    .onEach { result -> _todaySession.value = result }
    .flattenConcat(1) // Only process latest, cancel previous
    .flowOn(Dispatchers.IO)
    .stateIn(...)

// Call site: use _sessionQuery.emit(date) instead of getSessionForDate(date)
```

---

## Files to Modify

### 1. DashboardViewModel.kt

**Changes:**
- Add import: `kotlinx.coroutines.flow.flatMapLatest`
- Add import: `kotlinx.coroutines.flow.flow`
- Replace `_todaySession` initialization with flow-based approach
- Remove or simplify `getSessionForDate()` method
- Handle the date parameter from CalendarViewmodel

**Code Change:**

```kotlin
// Current (lines 60-61):
private val _todaySession = MutableStateFlow<Resource<TrainingSession?>>(Resource.Loading())
val todaySession: StateFlow<Resource<TrainingSession?>> = _todaySession.asStateFlow()

// Replace with:
private val _dateSelector = MutableStateFlow<LocalDate>(LocalDate.now())

val todaySession: StateFlow<Resource<TrainingSession?>> = _dateSelector
    .flatMapLatest { date ->
        flow {
            emit(Resource.Loading())
            exerciseRepository.getSessionForDate(
                Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
            ).collect { emit(it) }
        }
    }
    .flowOn(Dispatchers.IO)
    .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Resource.Loading()
    )
```

### 2. DashboardScreen.kt

**Changes:**
- Simplified LaunchedEffect - no manual getSessionForDate call needed
- The UI will automatically react to date changes

**Code Change:**

```kotlin
// Current (lines 137-138):
LaunchedEffect(selectedDate) {
    viewModel.getSessionForDate(selectedDate)
}

// Remove or simplify - the flow in ViewModel handles it automatically
// OR keep for explicit refresh and use flatMapLatest internally
```

### 3. CalendarViewmodel.kt

**Changes:**
- No changes needed - already uses MutableStateFlow

---

## Summary

| Current | Fix |
|---------|-----|
| `getSessionForDate()` launches new coroutine | Use `flatMapLatest` to cancel previous |
| Multiple concurrent queries | Only latest query runs |
| Race to update UI | Only latest result updates UI |

---

## Implementation Order

1. **DashboardViewModel** - Add flatMapLatest logic
2. **DashboardScreen** - Simplify or remove LaunchedEffect
3. **Test** - Verify rapid date tapping works correctly

---

## Testing Notes

After fix, verify:
- Tap rapidly between dates → UI shows correct (latest) date
- No visual flickering from stale results
- Flow properly cancels when new date selected