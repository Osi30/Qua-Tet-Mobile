package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.PageResponse
import com.semester7.quatet.data.model.ProductDTO
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductApiService {
    @GET("api/products")
    suspend fun getAllProducts(
        @Query("Search") search: String?,
        @Query("Categories") categories: List<Int>?,
        @Query("Sort") sort: String?,
        @Query("MinPrice") minPrice: Double?,
        @Query("MaxPrice") maxPrice: Double?,
        @Query("PageNumber") pageNumber: Int? = null,
        @Query("PageSize") pageSize: Int? = null
    ): BaseResponse<PageResponse<List<ProductDTO>>>
}