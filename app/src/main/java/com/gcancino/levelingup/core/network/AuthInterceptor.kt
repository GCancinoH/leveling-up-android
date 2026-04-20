package com.gcancino.levelingup.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AuthInterceptor : Interceptor {
    private val tokenReference = AtomicReference<TokenInfo>()

    private data class TokenInfo(val token: String, val expiry: Long)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            return chain.proceed(originalRequest)
        }

        val token = getValidToken(user)

        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return try {
            chain.proceed(newRequest)
        } catch (e: Exception) {
            Timber.tag("AuthInterceptor").e(e, "Request failed")
            throw e
        }
    }

    private fun getValidToken(user: com.google.firebase.auth.FirebaseUser): String {
        val now = System.currentTimeMillis()
        val currentInfo = tokenReference.get()

        // Return cached token if valid (with 5-minute buffer)
        if (currentInfo != null && now < currentInfo.expiry - TimeUnit.MINUTES.toMillis(5)) {
            return currentInfo.token
        }

        // Token expired or not present, fetch synchronously for the interceptor
        return synchronized(this) {
            val reCheckInfo = tokenReference.get()
            if (reCheckInfo != null && now < reCheckInfo.expiry - TimeUnit.MINUTES.toMillis(5)) {
                return@synchronized reCheckInfo.token
            }

            try {
                // Interceptor is running on a background thread (OkHttp Dispatcher), 
                // so blocking here is acceptable but should be minimized.
                val task = user.getIdToken(false)
                val idTokenResult = Tasks.await(task, 10, TimeUnit.SECONDS)
                val token = idTokenResult.token ?: throw IllegalStateException("Token is null")
                
                // Firebase tokens usually last 1 hour
                val expiry = idTokenResult.expirationTimestamp * 1000
                tokenReference.set(TokenInfo(token, expiry))
                
                token
            } catch (e: Exception) {
                Timber.tag("AuthInterceptor").e(e, "Failed to fetch Firebase ID token")
                // If fetch fails, try to use expired token as last resort or throw
                currentInfo?.token ?: throw e
            }
        }
    }
}