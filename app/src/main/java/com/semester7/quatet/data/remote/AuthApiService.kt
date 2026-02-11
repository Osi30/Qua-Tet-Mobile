package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.AuthResult
import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.LoginRequest
import com.semester7.quatet.data.model.MessageResponse
import com.semester7.quatet.data.model.RegisterRequest
import com.semester7.quatet.data.model.VerifyOtpRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): BaseResponse<AuthResult>

    @POST("api/auth/register/request-otp")
    suspend fun register(@Body request: RegisterRequest): BaseResponse<MessageResponse>

    @POST("api/auth/register/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): BaseResponse<AuthResult>
}
