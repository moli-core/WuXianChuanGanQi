package com.smarthome.app.core.network

import android.content.Context
import com.smarthome.app.core.datastore.TokenDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(context: Context) : Interceptor {

    private val tokenDataStore = TokenDataStore(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path in AUTH_WHITELIST) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { tokenDataStore.getToken() }

        return if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }

    companion object {
        val AUTH_WHITELIST = setOf("/api/auth/login", "/api/auth/register")
    }
}
