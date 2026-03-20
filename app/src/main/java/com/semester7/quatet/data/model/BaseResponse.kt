package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val status: Int,
    val msg: String? = null,
    val data: T
)
