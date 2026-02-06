package com.semester7.quatet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.ProductDetailDTO
import com.semester7.quatet.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductDetailViewModel : ViewModel() {
    private val repository = ProductRepository()

    // Trạng thái danh sách sản phẩm
    // Encapsulation (Mutable => có thể thay đổi)
    private val _product = MutableLiveData<ProductDetailDTO>()

    // Getter (Readonly)
    val product: LiveData<ProductDetailDTO> get() = _product

    // Trạng thái loading (Product)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Load Product
    fun fetchProduct(productId: Int) {
        // Sử dụng launch cho các hàm suspend
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getProductById(productId)

                if (response.status == 200) {
                    Log.d("API_DEBUG", "Sản phẩm: ${response.data}")
                    _product.postValue(response.data)
                }
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Lỗi kết nối: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}