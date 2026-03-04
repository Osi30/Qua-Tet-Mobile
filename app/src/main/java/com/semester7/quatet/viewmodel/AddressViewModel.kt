package com.semester7.quatet.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.AccountAddressDTO
import com.semester7.quatet.data.model.AddressRequest
import com.semester7.quatet.data.repository.AddressRepository
import kotlinx.coroutines.launch

class AddressViewModel : ViewModel() {

    private val repository = AddressRepository()

    private val _addresses = MutableLiveData<List<AccountAddressDTO>>(emptyList())
    val addresses: LiveData<List<AccountAddressDTO>> = _addresses

    private val _defaultAddress = MutableLiveData<AccountAddressDTO?>()
    val defaultAddress: LiveData<AccountAddressDTO?> = _defaultAddress

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    fun fetchAddresses() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _addresses.value = repository.getAddresses()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Không thể tải danh sách địa chỉ"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchDefaultAddress() {
        viewModelScope.launch {
            try {
                _defaultAddress.value = repository.getDefaultAddress()
            } catch (_: Exception) {
                _defaultAddress.value = null
            }
        }
    }

    fun createAddress(request: AddressRequest) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.createAddress(request)
                _successMessage.value = "Thêm địa chỉ thành công"
                fetchAddresses()
                fetchDefaultAddress()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Không thể thêm địa chỉ"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAddress(addressId: Int, request: AddressRequest) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.updateAddress(addressId, request)
                _successMessage.value = "Cập nhật địa chỉ thành công"
                fetchAddresses()
                fetchDefaultAddress()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Không thể cập nhật địa chỉ"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAddress(addressId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.deleteAddress(addressId)
                _successMessage.value = "Đã xóa địa chỉ"
                fetchAddresses()
                fetchDefaultAddress()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Không thể xóa địa chỉ"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setDefaultAddress(addressId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.setDefaultAddress(addressId)
                _successMessage.value = "Đã đặt địa chỉ mặc định"
                fetchAddresses()
                fetchDefaultAddress()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Không thể đặt mặc định"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
