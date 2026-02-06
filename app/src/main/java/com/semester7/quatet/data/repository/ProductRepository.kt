package com.semester7.quatet.data.repository

import com.semester7.quatet.data.remote.ProductApiService
import com.semester7.quatet.data.remote.RetrofitClient


class ProductRepository {
    private val apiService = RetrofitClient.createService(ProductApiService::class.java)

    suspend fun getProducts(
        search: String?,
        categories: List<Int>?,
        sort: String?,
        minPrice: Double?,
        maxPrice: Double?
    ) = apiService.getAllProducts(
        search = if (search.isNullOrBlank()) null else search,
        categories = if (categories.isNullOrEmpty()) null else categories,
        sort = sort,
        minPrice = minPrice,
        maxPrice = maxPrice
    )

    suspend fun getProductById(productId: Int)
    = apiService.getProductById(productId = productId)
}