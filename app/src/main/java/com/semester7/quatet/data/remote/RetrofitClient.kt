package com.semester7.quatet.data.remote

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

// Singleton (Start khi dùng lần đầu và chết khi ứng dụng bị kill)
object RetrofitClient {

    private const val BASE_URL = "http://14.225.207.221:5000"

    private val json = Json {
        // Tránh lỗi nếu API trả về thừa trường so với Model
        ignoreUnknownKeys = true
        // Bỏ qua lỗi định dạng nhỏ
        isLenient = true
    }

    // Xem Log trong Logcat
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var retrofit: Retrofit? = null

    // Khởi tạo RetrofitClient với Context (gọi 1 lần trong Application hoặc Activity đầu tiên)
    fun init(context: Context) {
        if (retrofit != null) return

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context.applicationContext))
            .addInterceptor(logging)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    // Hàm tiện ích để tạo các API Service
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit!!.create(serviceClass)
    }
}
