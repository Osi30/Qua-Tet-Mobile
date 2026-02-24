package com.semester7.quatet.viewmodel // Nhớ kiểm tra lại tên package của bạn nhé

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.OrderRequest
import com.semester7.quatet.data.repository.OrderRepository
import com.semester7.quatet.data.repository.PaymentRepository
import kotlinx.coroutines.launch
import android.util.Log

class CheckoutViewModel : ViewModel() {

    // Khởi tạo 2 kho dữ liệu mà chúng ta đã làm ở Bước 2
    private val orderRepository = OrderRepository()
    private val paymentRepository = PaymentRepository()

    // --- CÁC "CHIẾC HỘP" STATE ĐỂ ACTIVITY LẮNG NGHE ---

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // Hộp này chứa đường link VNPay trả về sau khi chuỗi gọi API thành công
    private val _paymentUrl = MutableLiveData<String?>()
    val paymentUrl: LiveData<String?> get() = _paymentUrl

    private val _createdOrderId = MutableLiveData<Int>()
    val createdOrderId: LiveData<Int> get() = _createdOrderId
    // --- HÀM XỬ LÝ LOGIC THANH TOÁN ---

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
                // 1. Đóng gói dữ liệu người dùng nhập thành Request
                Log.d("CHECKOUT_FLOW", "Đang tạo Order...")
                val orderRequest = OrderRequest(
                    customerName = name,
                    customerPhone = phone,
                    customerEmail = email,
                    customerAddress = address,
                    note = note
                )

                // 2. Gọi API Tạo đơn hàng
                val orderResponse = orderRepository.createOrder(orderRequest)

                // Kiểm tra xem server có trả về orderId không
                if (orderResponse != null) {
                    val newOrderId = orderResponse.orderId
                    Log.d("CHECKOUT_FLOW", "Tạo Order thành công! OrderID: $newOrderId")
                    _createdOrderId.value = newOrderId
                    // 3. Có orderId rồi, lập tức gọi tiếp API lấy link VNPay
                    Log.d("CHECKOUT_FLOW", "Đang lấy Link VNPay...")
                    // CHÚ Ý: Lúc này paymentResponse trả về là PaymentDTO (Object) chứ không phải String
                    val paymentResponse = paymentRepository.createPaymentUrl(newOrderId, "VNPAY")

                    if (paymentResponse != null) {
                        // Trích xuất link VNPay từ thuộc tính paymentUrl (thuộc tính bạn vừa thêm ở PaymentDTO)
                        val vnpayUrl = paymentResponse.paymentUrl

                        if (!vnpayUrl.isNullOrEmpty()) {
                            Log.d("CHECKOUT_FLOW", "Lấy Link thành công: $vnpayUrl")
                            // 4. Thành công mỹ mãn! Bỏ link VNPay vào hộp để UI mang đi mở web
                            _paymentUrl.value = vnpayUrl
                        } else {
                            Log.e("CHECKOUT_FLOW", "Không tìm thấy URL VNPay trong JSON. Hãy kiểm tra lại Swagger xem Backend trả về biến tên gì!")
                            _errorMessage.value = "Chưa tìm thấy link thanh toán từ hệ thống."
                        }
                    } else {
                        _errorMessage.value = "Không thể tạo liên kết thanh toán VNPay lúc này."
                    }
                } else {
                    _errorMessage.value = "Tạo đơn hàng thất bại. Vui lòng thử lại!"
                }

            } catch (e: retrofit2.HttpException) {
                // Nếu là lỗi từ Server (HTTP 400, 404, 500...)
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("CHECKOUT_FLOW", "Lỗi Server HTTP ${e.code()}: $errorBody")
                _errorMessage.value = "Lỗi Server: $errorBody"
            } catch (e: Exception) {
                Log.e("CHECKOUT_FLOW", "Lỗi Exception: ${e.message}")
                // Các lỗi khác như rớt mạng, sập app, lỗi parse JSON...
                _errorMessage.value = e.message ?: "Có lỗi xảy ra trong quá trình xử lý"
            } finally {
                // Luôn luôn tắt vòng xoay Loading dù thành công hay thất bại
                _isLoading.value = false
            }
        }
    }

    // Hàm dùng để reset lại thông báo lỗi (nếu cần)
    fun clearMessages() {
        _errorMessage.value = null
    }
}