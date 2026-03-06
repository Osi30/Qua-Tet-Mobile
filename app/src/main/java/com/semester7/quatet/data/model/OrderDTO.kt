package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderDTO(
    val orderId: Int? = null,    // Dùng khi tạo đơn hàng thành công (CreateOrder)
    val id: Int? = null,         // Mã đơn hàng trả về trong danh sách lịch sử
    val customername: String? = null,
    val totalPrice: Double? = null,
    val totalamount: Double? = null, // Tổng tiền đơn hàng
    val status: String? = null,        // Sửa thành String vì server trả về "PENDING", "CONFIRMED"...
    val orderDateTime: String? = null
    // Bạn có thể thêm các trường khác nếu API Swagger có trả về (ví dụ: danh sách sản phẩm)
)