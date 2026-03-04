package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StoreLocationDTO(
    val storeLocationId: Int,
    val name: String,
    val addressLine: String,
    val latitude: Double,
    val longitude: Double,
    val phoneNumber: String? = null,
    val openHoursText: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class DirectionToStoreRequest(
    val fromLat: Double,
    val fromLng: Double,
    val travelMode: String
)

@Serializable
data class DirectionToStoreResponse(
    val storeLocationId: Int,
    val url: String
)
