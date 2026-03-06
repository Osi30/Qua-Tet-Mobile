package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.OrderDTO
import com.semester7.quatet.data.model.OrderRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OrderApiService {

    @POST("api/orders")
    suspend fun createOrder(@Body request: OrderRequest): BaseResponse<OrderDTO>

    @GET("api/orders/my-orders")
    suspend fun getMyOrders(
        @Query("pageIndex") pageIndex: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): BaseResponse<List<OrderDTO>> // Trả lại List<OrderDTO> bình thường
}