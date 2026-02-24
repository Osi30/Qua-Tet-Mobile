package com.semester7.quatet.data.repository

import com.semester7.quatet.data.model.PaymentDTO
import com.semester7.quatet.data.model.PaymentRequest
import com.semester7.quatet.data.remote.PaymentApiService
import com.semester7.quatet.data.remote.RetrofitClient

class PaymentRepository {
    private val apiService = RetrofitClient.createService(PaymentApiService::class.java)

    // --- Hàm cũ ---
    suspend fun getPaymentByOrderId(orderId: Int): List<PaymentDTO>? {
        return apiService.getPaymentByOrderId(orderId).data
    }

    // --- HÀM MỚI THÊM ---
    // Hàm mới sau khi sửa
    suspend fun createPaymentUrl(orderId: Int, paymentMethod: String): PaymentDTO? {
        val request = PaymentRequest(orderId = orderId, paymentMethod = paymentMethod)
        return apiService.createPaymentUrl(request).data
    }
}