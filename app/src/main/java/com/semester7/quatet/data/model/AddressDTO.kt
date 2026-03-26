package com.semester7.quatet.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountAddressDTO(
    val accountAddressId: Int,
    val accountId: Int,
    val label: String,
    val customername: String? = null,
    val customerphone: String? = null,
    val customeremail: String? = null,
    val addressLine: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)

@Serializable
data class AddressRequest(
    val label: String,
    @SerialName("customername") val customername: String?,
    @SerialName("customerphone") val customerphone: String?,
    @SerialName("customeremail") val customeremail: String?,
    @SerialName("customerName") val customerName: String?,
    @SerialName("customerPhone") val customerPhone: String?,
    @SerialName("customerEmail") val customerEmail: String?,
    val addressLine: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean,
    val isActive: Boolean
)

@Serializable
data class SuccessResponse(
    val success: Boolean
)
