package com.semester7.quatet.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.OrderDTO
import com.semester7.quatet.data.repository.OrderRepository
import kotlinx.coroutines.launch

class OrderHistoryViewModel : ViewModel() {

    private val repository = OrderRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _orders = MutableLiveData<List<OrderDTO>?>()
    val orders: LiveData<List<OrderDTO>?> get() = _orders

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun fetchMyOrders() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Gọi tới hàm getMyOrders() đã tạo ở bước trước
                val result = repository.getMyOrders()
                _orders.value = result
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Không thể tải lịch sử đơn hàng"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}