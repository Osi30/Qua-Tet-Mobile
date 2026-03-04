package com.semester7.quatet.data.repository

import com.semester7.quatet.data.model.AccountAddressDTO
import com.semester7.quatet.data.model.AddressRequest
import com.semester7.quatet.data.model.SuccessResponse
import com.semester7.quatet.data.remote.AddressApiService
import com.semester7.quatet.data.remote.RetrofitClient

class AddressRepository {
    private val apiService = RetrofitClient.createService(AddressApiService::class.java)

    suspend fun getAddresses(): List<AccountAddressDTO> = apiService.getAddresses().data

    suspend fun getDefaultAddress(): AccountAddressDTO = apiService.getDefaultAddress().data

    suspend fun createAddress(request: AddressRequest): AccountAddressDTO =
        apiService.createAddress(request).data

    suspend fun updateAddress(addressId: Int, request: AddressRequest): AccountAddressDTO =
        apiService.updateAddress(addressId, request).data

    suspend fun deleteAddress(addressId: Int): SuccessResponse =
        apiService.deleteAddress(addressId).data

    suspend fun setDefaultAddress(addressId: Int): SuccessResponse =
        apiService.setDefaultAddress(addressId).data
}
