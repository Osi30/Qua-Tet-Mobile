package com.semester7.quatet.data.repository

import android.util.Log
import com.semester7.quatet.data.model.OrderDTO
import com.semester7.quatet.data.model.OrderRequest
import com.semester7.quatet.data.remote.OrderApiService
import com.semester7.quatet.data.remote.RetrofitClient

class OrderRepository {
    private val apiService = RetrofitClient.createService(OrderApiService::class.java)

    suspend fun createOrder(request: OrderRequest): OrderDTO? {
        return apiService.createOrder(request).data
    }

    suspend fun getMyOrders(): List<OrderDTO>? {
        Log.d("OrderRepository", "Đang gọi API lấy danh sách đơn hàng...")
        val response = apiService.getMyOrders()
        return response.data
    }
}