package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

// === Request DTOs ===

@Serializable
data class AddToCartRequest(
    val productId: Int,
    val quantity: Int
)

@Serializable
data class UpdateCartItemRequest(
    val quantity: Int
)

// === Response DTOs ===

@Serializable
data class CartItemResponse(
    val cartDetailId: Int,
    val productId: Int,
    val productName: String? = null,
    val sku: String? = null,
    val price: Double? = null,
    val quantity: Int? = null,
    val subTotal: Double = 0.0,
    val imageUrl: String? = null
)

@Serializable
data class CartResponse(
    val cartId: Int,
    val accountId: Int,
    val items: List<CartItemResponse> = emptyList(),
    val totalPrice: Double = 0.0,
    val discountValue: Double? = null,
    val finalPrice: Double = 0.0,
    val itemCount: Int = 0,
    val promotionCode: String? = null
)

@Serializable
data class CartCountResponse(
    val count: Int
)
