package com.semester7.quatet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.CategoryDTO
import com.semester7.quatet.data.model.ProductDTO
import com.semester7.quatet.data.repository.CategoryRepository
import com.semester7.quatet.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {
    private val repository = ProductRepository()
    private val categoryRepository = CategoryRepository()

    // Trạng thái danh sách sản phẩm
    // Encapsulation (Mutable => có thể thay đổi)
    private val _products = MutableLiveData<List<ProductDTO>>()

    // Getter (Readonly)
    val products: LiveData<List<ProductDTO>> get() = _products

    // Trạng thái loading (Product)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Trạng thái loading (Category)
    private val _isCategoryLoading = MutableLiveData<Boolean>()

    // Trạng thái
    private val _categories = MutableLiveData<List<CategoryDTO>>()
    val categories: LiveData<List<CategoryDTO>> get() = _categories

    // Trạng thái hiện tại (State)
    private var _currentSearch: String? = null
    private var _currentSort: String? = "default"
    fun getCurrentSort(): String? = _currentSort
    private var _currentMinPrice: Double? = null
    private var _currentMaxPrice: Double? = null
    var currentSelectedCategoryIds: List<Int> = emptyList()

    init {
        fetchProducts()
        fetchCategories()
    }

    fun fetchProducts() {
        // Sử dụng launch cho các hàm suspend
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getProducts(
                    search = _currentSearch,
                    categories = currentSelectedCategoryIds,
                    sort = _currentSort,
                    minPrice = _currentMinPrice,
                    maxPrice = _currentMaxPrice
                )

                if (response.status == 200) {
                    Log.d("API_DEBUG", "Các sản phẩm: ${response.data}")
                    _products.postValue(response.data.data)
                }
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Lỗi kết nối: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun fetchCategories() {
        viewModelScope.launch {
            _isCategoryLoading.value = true
            try {
                val response = categoryRepository.getCategories()

                if (response.status == 200) {
                    Log.d("API_DEBUG", "Các loại: ${response.data}")
                    _categories.postValue(response.data)
                }
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Lỗi kết nối: ${e.message}")
            } finally {
                _isCategoryLoading.postValue(false)
            }
        }
    }

    // Cập nhật giá trị mới nếu có truyền vào
    fun updateFilters(
        search: String? = null,
        sort: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        categories: List<Int>? = null
    ) {
        search?.let { _currentSearch = it }
        sort?.let { _currentSort = it }
        minPrice?.let { _currentMinPrice = it }
        maxPrice?.let { _currentMaxPrice = it }
        categories?.let { currentSelectedCategoryIds = it }

        fetchProducts()
    }
}