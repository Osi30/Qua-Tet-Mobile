package com.semester7.quatet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.CartResponse
import com.semester7.quatet.data.repository.CartRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class CartViewModel : ViewModel() {

    private val repository = CartRepository()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Giỏ hàng
    private val _cart = MutableLiveData<CartResponse?>()
    val cart: LiveData<CartResponse?> = _cart

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Thông báo thành công thông thường (Toast)
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Sự kiện bắn Notification (dùng để UI trigger NotificationHelper)
    private val _shouldShowNotification = MutableLiveData<String?>()
    val shouldShowNotification: LiveData<String?> = _shouldShowNotification

    // Lấy giỏ hàng
    fun fetchCart() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.getCart()
                _cart.value = result
            } catch (e: Exception) {
                _errorMessage.value = parseError(e, "Lỗi lấy giỏ hàng")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Thêm sản phẩm vào giỏ
    fun addItem(productId: Int, quantity: Int = 1) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.addItem(productId, quantity)
                _cart.value = result

                // Trigger thông báo hệ thống (Notification Badge)
                _shouldShowNotification.value = "Sản phẩm đã được thêm vào giỏ hàng thành công!"

                Log.d("CART_VM", "Add Item Success: Product ID $productId")
            } catch (e: Exception) {
                _errorMessage.value = parseError(e, "Lỗi thêm giỏ hàng")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Cập nhật số lượng
    fun updateItem(cartDetailId: Int, quantity: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.updateItem(cartDetailId, quantity)
                _cart.value = result
            } catch (e: Exception) {
                _errorMessage.value = parseError(e, "Lỗi cập nhật số lượng")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Xóa sản phẩm
    fun removeItem(cartDetailId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.removeItem(cartDetailId)
                _cart.value = result
                _successMessage.value = "Đã xóa sản phẩm khỏi giỏ"
            } catch (e: Exception) {
                _errorMessage.value = parseError(e, "Lỗi xóa sản phẩm")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Xóa toàn bộ giỏ hàng
    fun clearCart() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.clearCart()
                _cart.value = null
                _successMessage.value = "Đã làm trống giỏ hàng"
            } catch (e: Exception) {
                _errorMessage.value = parseError(e, "Lỗi xóa giỏ hàng")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
        _shouldShowNotification.value = null
    }

    /**
     * Parse lỗi từ Server theo chuẩn BaseResponse
     */
    private fun parseError(e: Exception, defaultMsg: String): String {
        Log.e("CART_DEBUG", "$defaultMsg: ${e.message}")

        if (e is HttpException) {
            return try {
                val errorJson = e.response()?.errorBody()?.string()
                if (errorJson != null) {
                    val baseResponse = json.decodeFromString<BaseResponse<Unit>>(errorJson)
                    baseResponse.msg
                } else defaultMsg
            } catch (_: Exception) {
                "Lỗi hệ thống (${e.code()})"
            }
        }
        return e.localizedMessage ?: defaultMsg
    }
}