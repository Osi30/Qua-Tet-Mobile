package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.AddToCartRequest
import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.CartCountResponse
import com.semester7.quatet.data.model.CartResponse
import com.semester7.quatet.data.model.MessageResponse
import com.semester7.quatet.data.model.UpdateCartItemRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CartApiService {

    @GET("api/carts")
    suspend fun getCart(): BaseResponse<CartResponse>

    @GET("api/carts/count")
    suspend fun getCartItemCount(): BaseResponse<CartCountResponse>

    @POST("api/carts/items")
    suspend fun addItem(@Body request: AddToCartRequest): BaseResponse<CartResponse>

    @PUT("api/carts/items/{cartDetailId}")
    suspend fun updateItem(
        @Path("cartDetailId") cartDetailId: Int,
        @Body request: UpdateCartItemRequest
    ): BaseResponse<CartResponse>

    @DELETE("api/carts/items/{cartDetailId}")
    suspend fun removeItem(@Path("cartDetailId") cartDetailId: Int): BaseResponse<CartResponse>

    @DELETE("api/carts/clear")
    suspend fun clearCart(): BaseResponse<MessageResponse>
}
