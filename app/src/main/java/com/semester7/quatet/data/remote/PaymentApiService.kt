package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.PaymentDTO
import com.semester7.quatet.data.model.PaymentRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentApiService {

    // --- Hàm cũ (Dùng cho màn hình Bill) ---
    @GET("api/payments/order/{orderId}")
    suspend fun getPaymentByOrderId(
        @Path("orderId") orderId: Int
    ): BaseResponse<List<PaymentDTO>>

    // --- HÀM MỚI THÊM: Dùng để lấy link VNPay ---
    @POST("api/payments")
    suspend fun createPaymentUrl(
        @Body request: PaymentRequest
    ): BaseResponse<PaymentDTO> // Đã sửa String thành PaymentDTO
}