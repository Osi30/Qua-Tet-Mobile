package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.ProductDTO
import retrofit2.http.GET

interface ProductApiService {
    @GET("api/Products")
    suspend fun getAllProducts(): BaseResponse<List<ProductDTO>>
}