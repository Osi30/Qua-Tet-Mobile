package com.semester7.quatet.data.repository

import com.semester7.quatet.data.remote.ProductApiService
import com.semester7.quatet.data.remote.RetrofitClient


class ProductRepository {
    private val apiService = RetrofitClient.createService(ProductApiService::class.java)

    suspend fun getProducts() = apiService.getAllProducts()
}