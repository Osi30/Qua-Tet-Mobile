package com.semester7.quatet.viewmodel // Sửa lại package nếu cần cho khớp với cấu trúc thư mục của bạn

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.PaymentDTO
import com.semester7.quatet.data.repository.PaymentRepository
import kotlinx.coroutines.launch

class PaymentViewModel : ViewModel() {

    // Khởi tạo Repository để nhờ nó đi lấy data
    private val repository = PaymentRepository()

    // --- CÁC "CHIẾC HỘP" LIVEDATA ĐỂ BÁO CÁO TÌNH TRẠNG CHO MÀN HÌNH ---

    // 1. Báo cáo trạng thái Loading (Đang xoay vòng vòng chờ mạng)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // 2. Báo cáo dữ liệu Thành công (Chứa danh sách thanh toán trả về từ API)
    private val _paymentResult = MutableLiveData<List<PaymentDTO>?>()
    val paymentResult: LiveData<List<PaymentDTO>?> get() = _paymentResult

    // 3. Báo cáo Lỗi (Chứa câu thông báo lỗi nếu rớt mạng, lỗi server...)
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // --- HÀM GỌI API ---

    fun getPaymentInfo(orderId: Int) {
        // Bắt đầu gọi API -> Bật cờ Loading lên true
        _isLoading.value = true
        _errorMessage.value = null // Xóa lỗi cũ nếu có

        // viewModelScope.launch giúp chạy ngầm, không làm đơ giao diện người dùng
        viewModelScope.launch {
            try {
                // Nhờ repository đi hỏi server lấy bill của orderId này
                val result = repository.getPaymentByOrderId(orderId)

                // Cất dữ liệu lấy được vào chiếc hộp paymentResult
                _paymentResult.value = result
            } catch (e: Exception) {
                // Nếu xảy ra lỗi (ví dụ: tắt WiFi, server chết) -> Quăng lỗi vào hộp errorMessage
                _errorMessage.value = e.message ?: "Có lỗi xảy ra khi tải thông tin thanh toán"
            } finally {
                // Dù thành công hay thất bại thì cũng phải tắt vòng xoay Loading
                _isLoading.value = false
            }
        }
    }
}