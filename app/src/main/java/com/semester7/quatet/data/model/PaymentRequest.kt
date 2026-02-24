package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentRequest(
    val orderId: Int,
    val paymentMethod: String // Ở đây chúng ta sẽ truyền cứng là "VNPAY"
)