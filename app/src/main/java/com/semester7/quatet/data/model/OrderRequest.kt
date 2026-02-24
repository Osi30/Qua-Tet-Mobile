package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val customerAddress: String,
    val note: String? = null,           // Có thể null vì ghi chú là tùy chọn
    val promotionCode: String? = null   // Có thể null nếu không có mã giảm giá
)