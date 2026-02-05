package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T> (
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val pageSize: Int,
    val data: T
)