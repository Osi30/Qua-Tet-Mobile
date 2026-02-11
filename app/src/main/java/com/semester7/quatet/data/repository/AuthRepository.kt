package com.semester7.quatet.data.repository

import com.semester7.quatet.data.model.LoginRequest
import com.semester7.quatet.data.model.RegisterRequest
import com.semester7.quatet.data.model.VerifyOtpRequest
import com.semester7.quatet.data.remote.AuthApiService
import com.semester7.quatet.data.remote.RetrofitClient

class AuthRepository {
    private val apiService = RetrofitClient.createService(AuthApiService::class.java)

    suspend fun login(username: String, password: String) =
        apiService.login(LoginRequest(username = username, password = password)).data

    suspend fun register(
        username: String,
        password: String,
        email: String,
        fullname: String?,
        phone: String?
    ) = apiService.register(
        RegisterRequest(
            username = username,
            password = password,
            email = email,
            fullname = fullname,
            phone = phone
        )
    ).data

    suspend fun verifyOtp(username: String, otp: String) =
        apiService.verifyOtp(VerifyOtpRequest(username = username, otp = otp)).data
}
