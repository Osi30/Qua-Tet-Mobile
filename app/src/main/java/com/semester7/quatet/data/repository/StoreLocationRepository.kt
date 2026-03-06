package com.semester7.quatet.data.repository

import com.semester7.quatet.data.model.DirectionToStoreRequest
import com.semester7.quatet.data.model.DirectionToStoreResponse
import com.semester7.quatet.data.model.StoreLocationDTO
import com.semester7.quatet.data.remote.RetrofitClient
import com.semester7.quatet.data.remote.StoreLocationApiService

class StoreLocationRepository {
    private val apiService = RetrofitClient.createService(StoreLocationApiService::class.java)

    suspend fun getActiveStoreLocations(): List<StoreLocationDTO> {
        return apiService.getActiveStoreLocations().data
    }

    suspend fun getDirectionsToStore(
        storeLocationId: Int,
        fromLat: Double,
        fromLng: Double,
        travelMode: String
    ): DirectionToStoreResponse {
        return apiService.getDirectionsToStore(
            storeLocationId = storeLocationId,
            request = DirectionToStoreRequest(
                fromLat = fromLat,
                fromLng = fromLng,
                travelMode = travelMode
            )
        ).data
    }
}
