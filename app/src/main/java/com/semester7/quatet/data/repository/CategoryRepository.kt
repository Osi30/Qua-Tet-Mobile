package com.semester7.quatet.data.repository

import com.semester7.quatet.data.remote.CategoryApiService
import com.semester7.quatet.data.remote.RetrofitClient

class CategoryRepository {
    private val categoryService = RetrofitClient.createService(CategoryApiService::class.java)

    suspend fun getCategories() = categoryService.getAllCategories()
}