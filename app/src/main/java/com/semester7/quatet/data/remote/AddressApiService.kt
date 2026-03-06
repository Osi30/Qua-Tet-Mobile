package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.AccountAddressDTO
import com.semester7.quatet.data.model.AddressRequest
import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.SuccessResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AddressApiService {

    @GET("api/account/addresses")
    suspend fun getAddresses(): BaseResponse<List<AccountAddressDTO>>

    @GET("api/account/addresses/default")
    suspend fun getDefaultAddress(): BaseResponse<AccountAddressDTO>

    @POST("api/account/addresses")
    suspend fun createAddress(@Body request: AddressRequest): BaseResponse<AccountAddressDTO>

    @PUT("api/account/addresses/{addressId}")
    suspend fun updateAddress(
        @Path("addressId") addressId: Int,
        @Body request: AddressRequest
    ): BaseResponse<AccountAddressDTO>

    @DELETE("api/account/addresses/{addressId}")
    suspend fun deleteAddress(@Path("addressId") addressId: Int): BaseResponse<SuccessResponse>

    @PUT("api/account/addresses/{addressId}/default")
    suspend fun setDefaultAddress(@Path("addressId") addressId: Int): BaseResponse<SuccessResponse>
}
