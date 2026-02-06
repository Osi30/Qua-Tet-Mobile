package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductDetailDTO (
    val productid: Int,
    val productname: String?,
    val description: String?,
    val price: Double?,
    val status: String?,
    val imageUrl: String?,
    val totalQuantity: Int?
)