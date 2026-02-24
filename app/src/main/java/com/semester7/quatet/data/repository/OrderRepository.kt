package com.semester7.quatet.data.repository

import com.semester7.quatet.data.model.OrderDTO
import com.semester7.quatet.data.model.OrderRequest
import com.semester7.quatet.data.remote.OrderApiService
import com.semester7.quatet.data.remote.RetrofitClient

class OrderRepository {
    private val apiService = RetrofitClient.createService(OrderApiService::class.java)

    suspend fun createOrder(request: OrderRequest): OrderDTO? {
        // Trả về data (chứa orderId)
        return apiService.createOrder(request).data
    }
}