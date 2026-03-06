package com.semester7.quatet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.AccountAddressDTO
import com.semester7.quatet.data.model.OrderRequest
import com.semester7.quatet.data.repository.AddressRepository
import com.semester7.quatet.data.repository.OrderRepository
import com.semester7.quatet.data.repository.PaymentRepository
import kotlinx.coroutines.launch

class CheckoutViewModel : ViewModel() {

    private val orderRepository = OrderRepository()
    private val paymentRepository = PaymentRepository()
    private val addressRepository = AddressRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _paymentUrl = MutableLiveData<String?>()
    val paymentUrl: LiveData<String?> get() = _paymentUrl

    private val _createdOrderId = MutableLiveData<Int>()
    val createdOrderId: LiveData<Int> get() = _createdOrderId

    private val _defaultAddress = MutableLiveData<AccountAddressDTO?>()
    val defaultAddress: LiveData<AccountAddressDTO?> get() = _defaultAddress

    fun loadDefaultAddress() {
        viewModelScope.launch {
            try {
                _defaultAddress.value = addressRepository.getDefaultAddress()
            } catch (_: Exception) {
                _defaultAddress.value = null
            }
        }
    }

    fun processCheckout(
        name: String,
        phone: String,
        email: String,
        address: String,
        note: String?
    ) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d("CHECKOUT_FLOW", "Dang tao Order...")
                val orderRequest = OrderRequest(
                    customerName = name,
                    customerPhone = phone,
                    customerEmail = email,
                    customerAddress = address,
                    note = note
                )

                val orderResponse = orderRepository.createOrder(orderRequest)

                if (orderResponse != null) {
                    val newOrderId = orderResponse.orderId
                    Log.d("CHECKOUT_FLOW", "Tao Order thanh cong! OrderID: $newOrderId")
                    _createdOrderId.value = newOrderId

                    Log.d("CHECKOUT_FLOW", "Dang lay Link VNPay...")
                    val paymentResponse = paymentRepository.createPaymentUrl(newOrderId, "VNPAY")

                    if (paymentResponse != null) {
                        val vnpayUrl = paymentResponse.paymentUrl

                        if (!vnpayUrl.isNullOrEmpty()) {
                            Log.d("CHECKOUT_FLOW", "Lay Link thanh cong: $vnpayUrl")
                            _paymentUrl.value = vnpayUrl
                        } else {
                            _errorMessage.value = "Chua tim thay link thanh toan tu he thong."
                        }
                    } else {
                        _errorMessage.value = "Khong the tao lien ket thanh toan VNPay luc nay."
                    }
                } else {
                    _errorMessage.value = "Tao don hang that bai. Vui long thu lai!"
                }

            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("CHECKOUT_FLOW", "Loi Server HTTP ${e.code()}: $errorBody")
                _errorMessage.value = "Loi Server: $errorBody"
            } catch (e: Exception) {
                Log.e("CHECKOUT_FLOW", "Loi Exception: ${e.message}")
                _errorMessage.value = e.message ?: "Co loi xay ra trong qua trinh xu ly"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
    }
}
