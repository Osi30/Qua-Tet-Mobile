package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

// === REQUEST DTOs ===

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val fullname: String? = null,
    val phone: String? = null
)

@Serializable
data class VerifyOtpRequest(
    val username: String,
    val otp: String
)

// === RESPONSE DTOs ===

@Serializable
data class AuthResult(
    val token: String,
    val accountId: Int,
    val username: String,
    val email: String? = null,
    val role: String? = null
)

@Serializable
data class MessageResponse(
    val message: String
)

// Response lỗi từ ExceptionMiddleware
@Serializable
data class ErrorResponse(
    val status: Int,
    val msg: String,
    val data: String? = null
)
