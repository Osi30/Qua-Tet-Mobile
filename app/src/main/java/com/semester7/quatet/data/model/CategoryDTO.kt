package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDTO(
    val categoryid: Int,
    val categoryname: String
)