package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderDTO(
    val orderId: Int // Đây là "chìa khóa" quan trọng nhất để truyền sang API Thanh toán
    // Có thể server trả về nhiều trường khác nữa (như totalAmount, status...),
    // nhưng nhờ thuộc tính ignoreUnknownKeys của JSON, ta chỉ khai báo những gì ta cần lấy.
)