package com.semester7.quatet.ui.activities

import android.graphics.Color
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

    private lateinit var binding: ActivityBillingBinding
    private val viewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()

        val orderId = intent.getIntExtra("EXTRA_ORDER_ID", -1)
        if (orderId != -1) {
            viewModel.getPaymentInfo(orderId)
        } else {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnHome.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.paymentResult.observe(this) { paymentList ->
            if (!paymentList.isNullOrEmpty()) {
                val payment = paymentList[0]

                // Đổ dữ liệu cơ bản
                binding.tvOrderId.text = "#${payment.orderId}"
                binding.tvPaymentMethod.text = payment.type ?: "Không rõ"

                val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                binding.tvAmount.text = format.format(payment.amount)

                // TÙY BIẾN GIAO DIỆN THEO TRẠNG THÁI
                if (payment.status == "SUCCESS") {
                    binding.tvStatus.text = "THÀNH CÔNG!"
                    binding.tvStatus.setTextColor(Color.parseColor("#4CAF50"))

                    // Đổi màu Icon thành Xanh
                    binding.ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
                    binding.ivStatusIcon.setColorFilter(Color.parseColor("#4CAF50"))

                    // Hiện lời cảm ơn
                    binding.tvThankYou.visibility = View.VISIBLE
                } else {
                    binding.tvStatus.text = "GIAO DỊCH THẤT BẠI"
                    binding.tvStatus.setTextColor(Color.parseColor("#D32F2F"))

                    // Đổi Icon thành Dấu cảnh báo màu Đỏ
                    binding.ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                    binding.ivStatusIcon.setColorFilter(Color.parseColor("#D32F2F"))

                    // Ẩn lời cảm ơn
                    binding.tvThankYou.visibility = View.GONE
                }
            } else {
                binding.tvStatus.text = "Không tìm thấy thông tin"
            }
        }

        viewModel.errorMessage.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                binding.tvStatus.text = "Lỗi kết nối mạng"
                binding.tvStatus.setTextColor(Color.parseColor("#D32F2F"))
            }
        }
    }
}