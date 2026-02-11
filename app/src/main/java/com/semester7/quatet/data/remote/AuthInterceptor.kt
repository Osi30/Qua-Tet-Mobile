package com.semester7.quatet.data.remote

import android.content.Context
import com.semester7.quatet.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = SessionManager.getToken(context)

        // Nếu không có token thì gửi request gốc
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Gắn token vào header Authorization
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
