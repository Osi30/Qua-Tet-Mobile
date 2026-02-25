package com.semester7.quatet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.ProductDetailDTO
import com.semester7.quatet.data.repository.CartRepository
import com.semester7.quatet.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductDetailViewModel : ViewModel() {
    private val repository = ProductRepository()

    // Khởi tạo thêm kho dữ liệu Giỏ hàng để phục vụ nút Mua Ngay
    private val cartRepository = CartRepository()

    private val _product = MutableLiveData<ProductDetailDTO>()
    val product: LiveData<ProductDetailDTO> get() = _product

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // --- HỘP TÍN HIỆU CHO LUỒNG MUA NGAY ---
    private val _buyNowSuccess = MutableLiveData<Boolean>()
    val buyNowSuccess: LiveData<Boolean> get() = _buyNowSuccess

    // Hàm gọi API chi tiết sản phẩm
    fun fetchProduct(productId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getProductById(productId)
                if (response.status == 200) {
                    _product.postValue(response.data)
                }
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Lỗi kết nối: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // --- HÀM XỬ LÝ NÚT MUA NGAY ---
    fun buyNow(productId: Int, quantity: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Gọi API ném thẳng sản phẩm vào giỏ hàng
                cartRepository.addItem(productId, quantity)

                // 2. Thành công! Phát tín hiệu cho UI biết để nhảy trang
                _buyNowSuccess.value = true
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Lỗi Mua Ngay: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Hàm reset lại trạng thái để tránh bị nhảy trang 2 lần khi user ấn nút Back
    fun resetBuyNowState() {
        _buyNowSuccess.value = false
    }
}