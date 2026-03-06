package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.DirectionToStoreRequest
import com.semester7.quatet.data.model.DirectionToStoreResponse
import com.semester7.quatet.data.model.StoreLocationDTO
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StoreLocationApiService {

    @GET("api/store-locations/active")
    suspend fun getActiveStoreLocations(): BaseResponse<List<StoreLocationDTO>>

    @POST("api/directions/to-store/{storeLocationId}")
    suspend fun getDirectionsToStore(
        @Path("storeLocationId") storeLocationId: Int,
        @Body request: DirectionToStoreRequest
    ): BaseResponse<DirectionToStoreResponse>
}
