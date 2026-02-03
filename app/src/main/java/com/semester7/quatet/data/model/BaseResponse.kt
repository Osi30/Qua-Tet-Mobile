package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class BaseResponse<T>(
    val status: Int,
    val msg: String,
    val data: T
)