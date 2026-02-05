package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.CategoryDTO
import retrofit2.http.GET

interface CategoryApiService {

    @GET("/api/categories")
    suspend fun getAllCategories() : BaseResponse<List<CategoryDTO>>
}