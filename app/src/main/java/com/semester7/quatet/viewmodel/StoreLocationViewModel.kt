package com.semester7.quatet.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.StoreLocationDTO
import com.semester7.quatet.data.repository.StoreLocationRepository
import kotlinx.coroutines.launch

class StoreLocationViewModel : ViewModel() {

    private val repository = StoreLocationRepository()

    private val _locations = MutableLiveData<List<StoreLocationDTO>>(emptyList())
    val locations: LiveData<List<StoreLocationDTO>> = _locations

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _directionUrl = MutableLiveData<String?>()
    val directionUrl: LiveData<String?> = _directionUrl

    fun fetchLocations() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _locations.value = repository.getActiveStoreLocations()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Không thể tải danh sách cửa hàng"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestDirections(
        storeLocationId: Int,
        fromLat: Double,
        fromLng: Double,
        travelMode: String
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getDirectionsToStore(
                    storeLocationId = storeLocationId,
                    fromLat = fromLat,
                    fromLng = fromLng,
                    travelMode = travelMode
                )
                _directionUrl.value = response.url
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Không thể lấy chỉ đường"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _directionUrl.value = null
    }
}
