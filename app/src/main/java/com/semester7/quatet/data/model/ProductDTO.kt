package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductDTO(
    val productid: Int,
    val categoryid: Int?,
    val configid: Int?,
    val accountid: Int?,
    val sku: String?,
    val productname: String?,
    val description: String?,
    val price: Double,
    val status: String?,
    val imageUrl: String?,
    val unit: Double
)