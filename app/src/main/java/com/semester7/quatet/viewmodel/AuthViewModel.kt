package com.semester7.quatet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.AuthResult
import com.semester7.quatet.data.repository.AuthRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.semester7.quatet.data.model.ErrorResponse
import retrofit2.HttpException

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    // Kết quả Login / Verify OTP
    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> get() = _authResult

    // Kết quả Register (gửi OTP thành công)
    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> get() = _registerSuccess

    // Trạng thái loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Thông báo lỗi
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // Parse lỗi từ Backend (ExceptionMiddleware trả về JSON)
    private fun parseError(e: HttpException): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody != null) {
                val json = Json { ignoreUnknownKeys = true }
                val error = json.decodeFromString<ErrorResponse>(errorBody)
                error.msg
            } else {
                "Lỗi không xác định"
            }
        } catch (ex: Exception) {
            e.message ?: "Lỗi không xác định"
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.login(username, password)
                Log.d("AUTH_DEBUG", "Login thành công: ${result.username}")
                _authResult.postValue(result)
            } catch (e: HttpException) {
                val msg = parseError(e)
                Log.e("AUTH_DEBUG", "Login lỗi: $msg")
                _errorMessage.postValue(msg)
            } catch (e: Exception) {
                Log.e("AUTH_DEBUG", "Login lỗi: ${e.message}")
                _errorMessage.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun register(
        username: String,
        password: String,
        email: String,
        fullname: String?,
        phone: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.register(username, password, email, fullname, phone)
                Log.d("AUTH_DEBUG", "Register thành công, OTP đã gửi")
                _registerSuccess.postValue(true)
            } catch (e: HttpException) {
                val msg = parseError(e)
                Log.e("AUTH_DEBUG", "Register lỗi: $msg")
                _errorMessage.postValue(msg)
            } catch (e: Exception) {
                Log.e("AUTH_DEBUG", "Register lỗi: ${e.message}")
                _errorMessage.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun verifyOtp(username: String, otp: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.verifyOtp(username, otp)
                Log.d("AUTH_DEBUG", "Xác thực OTP thành công: ${result.username}")
                _authResult.postValue(result)
            } catch (e: HttpException) {
                val msg = parseError(e)
                Log.e("AUTH_DEBUG", "OTP lỗi: $msg")
                _errorMessage.postValue(msg)
            } catch (e: Exception) {
                Log.e("AUTH_DEBUG", "OTP lỗi: ${e.message}")
                _errorMessage.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
