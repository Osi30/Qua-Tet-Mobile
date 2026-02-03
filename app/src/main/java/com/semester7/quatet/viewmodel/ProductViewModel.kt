package com.semester7.quatet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.ProductDTO
import com.semester7.quatet.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {
    private val repository = ProductRepository()

    // Trạng thái danh sách sản phẩm
    // Encapsulation (Mutable => có thể thay đổi)
    private val _products = MutableLiveData<List<ProductDTO>>()
    // Getter (Readonly)
    val products: LiveData<List<ProductDTO>> get() = _products

    // Trạng thái loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        // Sử dụng launch cho các hàm suspend
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getProducts()

                if (response.status == 200) {
                    Log.d("API_DEBUG", "Các sản phẩm: ${response.data}")
                    _products.postValue(response.data)
                }
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Lỗi kết nối: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}