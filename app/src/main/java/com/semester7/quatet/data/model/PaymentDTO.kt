package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentDTO(
    val paymentId: Int,
    val orderId: Int,
    val walletId: Int? = null,
    val amount: Double,
    val status: String? = null,       // Thêm ? = null cho an toàn
    val type: String? = null,         // ĐÃ SỬA CHỖ NÀY THEO LỖI
    val paymentMethod: String? = null,
    val isPayOnline: Boolean? = null, // ĐÃ SỬA CHỖ NÀY THEO LỖI
    val transactionNo: String? = null,
    val createdDate: String? = null,
    val paymentUrl: String? = null
)