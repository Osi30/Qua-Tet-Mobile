package com.semester7.quatet.ui.activities // Nhớ kiểm tra lại package cho khớp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.databinding.ActivityBillingBinding
import com.semester7.quatet.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.util.Locale

class BillingActivity : AppCompatActivity() {

    // Khởi tạo ViewBinding để móc nối tới file activity_billing.xml
    private lateinit var binding: ActivityBillingBinding

    // Khởi tạo ViewModel cực gọn nhờ delegate 'by viewModels()'
    private val viewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BẮT ĐẦU LUỒNG CHẠY:
        // 1. Cài đặt các sự kiện click nút bấm
        setupListeners()

        // 2. Lắng nghe dữ liệu (Observe) từ ViewModel
        observeViewModel()

        // 3. Ra lệnh cho ViewModel gọi API.
        // Giả sử mã đơn hàng (orderId) là 1 (như trong Swagger của bạn).
        // Thực tế sau này, số 1 này sẽ được truyền từ màn hình Giỏ hàng sang qua Intent.
        val orderId = intent.getIntExtra("EXTRA_ORDER_ID", 1)
        viewModel.getPaymentInfo(orderId)
    }

    private fun setupListeners() {
        // Xử lý sự kiện bấm nút "Về Trang Chủ"
        binding.btnHome.setOnClickListener {
            // Tạm thời đóng màn hình này lại.
            // Sau này bạn có thể dùng Intent để chuyển hẳn về MainActivity
            finish()
        }
    }

    private fun observeViewModel() {
        // --- LẮNG NGHE TRẠNG THÁI LOADING ---
        viewModel.isLoading.observe(this) { isLoading ->
            // Nếu đang tải -> hiện ProgressBar xoay xoay. Nếu xong rồi -> ẩn đi.
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // --- LẮNG NGHE KẾT QUẢ DATA THÀNH CÔNG ---
        viewModel.paymentResult.observe(this) { paymentList ->
            // Kiểm tra xem danh sách trả về có bị rỗng không
            if (!paymentList.isNullOrEmpty()) {
                // Lấy giao dịch đầu tiên trong danh sách
                val payment = paymentList[0]

                // Đổ dữ liệu lên giao diện
                binding.tvOrderId.text = "#${payment.orderId}"
                binding.tvPaymentMethod.text = payment.type ?: "Không rõ"

                // Format số tiền thành định dạng VNĐ (VD: 1.180.000 ₫)
                val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                binding.tvAmount.text = format.format(payment.amount)

                // Đổi màu sắc, câu chữ tùy theo trạng thái giao dịch
                if (payment.status == "SUCCESS") {
                    binding.tvStatus.text = "GIAO DỊCH THÀNH CÔNG"
                    // Màu xanh lá
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    binding.tvStatus.text = "GIAO DỊCH THẤT BẠI"
                    // Màu đỏ đô
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#690000"))
                }
            } else {
                binding.tvStatus.text = "Không tìm thấy thông tin thanh toán"
            }
        }

        // --- LẮNG NGHE NẾU CÓ LỖI XẢY RA ---
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (errorMsg != null) {
                // Hiển thị thông báo nhỏ bóp lên (Toast)
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                binding.tvStatus.text = "Lỗi kết nối mạng"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#690000"))
            }
        }
    }
}