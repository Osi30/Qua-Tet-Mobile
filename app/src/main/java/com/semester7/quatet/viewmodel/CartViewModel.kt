package com.semester7.quatet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.CartResponse
import com.semester7.quatet.data.repository.CartRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class CartViewModel : ViewModel() {

    private val repository = CartRepository()
    private val json = Json { ignoreUnknownKeys = true }

    // Giỏ hàng
    private val _cart = MutableLiveData<CartResponse?>()
    val cart: LiveData<CartResponse?> = _cart

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Thông báo thành công (dùng cho add/update/remove)
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Lấy giỏ hàng
    fun fetchCart() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = repository.getCart()
                _cart.value = result
            } catch (e: Exception) {
                Log.e("CART_DEBUG", "Lỗi lấy giỏ hàng: ${e.message}")
                _errorMessage.value = parseError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Thêm sản phẩm vào giỏ
    fun addItem(productId: Int, quantity: Int = 1) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = repository.addItem(productId, quantity)
                _cart.value = result
                _successMessage.value = "Đã thêm vào giỏ hàng"
            } catch (e: Exception) {
                Log.e("CART_DEBUG", "Lỗi thêm giỏ hàng: ${e.message}")
                _errorMessage.value = parseError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Cập nhật số lượng
    fun updateItem(cartDetailId: Int, quantity: Int) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = repository.updateItem(cartDetailId, quantity)
                _cart.value = result
            } catch (e: Exception) {
                Log.e("CART_DEBUG", "Lỗi cập nhật: ${e.message}")
                _errorMessage.value = parseError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Xóa sản phẩm
    fun removeItem(cartDetailId: Int) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = repository.removeItem(cartDetailId)
                _cart.value = result
                _successMessage.value = "Đã xóa sản phẩm"
            } catch (e: Exception) {
                Log.e("CART_DEBUG", "Lỗi xóa: ${e.message}")
                _errorMessage.value = parseError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Xóa toàn bộ giỏ hàng
    fun clearCart() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                repository.clearCart()
                _cart.value = null
                _successMessage.value = "Đã xóa toàn bộ giỏ hàng"
            } catch (e: Exception) {
                Log.e("CART_DEBUG", "Lỗi xóa giỏ hàng: ${e.message}")
                _errorMessage.value = parseError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Clear messages sau khi hiển thị
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    private fun parseError(e: Exception): String {
        val errorBody = e.message ?: "Lỗi không xác định"
        return try {
            val baseResponse = json.decodeFromString<com.semester7.quatet.data.model.BaseResponse<kotlinx.serialization.json.JsonElement>>(errorBody)
            baseResponse.msg
        } catch (_: Exception) {
            errorBody
        }
    }
}
