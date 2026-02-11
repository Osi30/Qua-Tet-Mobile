package com.semester7.quatet.data.repository

import com.semester7.quatet.data.model.AddToCartRequest
import com.semester7.quatet.data.model.UpdateCartItemRequest
import com.semester7.quatet.data.remote.CartApiService
import com.semester7.quatet.data.remote.RetrofitClient

class CartRepository {

    private val apiService = RetrofitClient.createService(CartApiService::class.java)

    suspend fun getCart() = apiService.getCart().data

    suspend fun getCartItemCount() = apiService.getCartItemCount().data

    suspend fun addItem(productId: Int, quantity: Int) =
        apiService.addItem(AddToCartRequest(productId = productId, quantity = quantity)).data

    suspend fun updateItem(cartDetailId: Int, quantity: Int) =
        apiService.updateItem(cartDetailId, UpdateCartItemRequest(quantity = quantity)).data

    suspend fun removeItem(cartDetailId: Int) =
        apiService.removeItem(cartDetailId).data

    suspend fun clearCart() = apiService.clearCart().data
}
