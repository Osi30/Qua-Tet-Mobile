package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.OrderDTO
import com.semester7.quatet.data.model.OrderRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface OrderApiService {

    // Gọi API Tạo đơn hàng
    @POST("api/orders")
    suspend fun createOrder(@Body request: OrderRequest): BaseResponse<OrderDTO>

}