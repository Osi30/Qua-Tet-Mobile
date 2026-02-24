package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.databinding.ActivityCheckoutBinding
import com.semester7.quatet.viewmodel.CheckoutViewModel
import java.text.NumberFormat
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val viewModel: CheckoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Nhận tổng tiền từ màn hình Cart truyền sang
        val totalPrice = intent.getDoubleExtra("EXTRA_TOTAL_PRICE", 0.0)
        displayOrderSummary(totalPrice)

        // 2. Cài đặt sự kiện nút bấm
        setupListeners()

        // 3. Lắng nghe trạng thái từ ViewModel
        observeViewModel()
    }

    private fun displayOrderSummary(totalPrice: Double) {
        val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        val formattedPrice = format.format(totalPrice)

        // Đổ lên giao diện
        binding.tvCheckoutSubtotal.text = formattedPrice
        binding.tvCheckoutTotal.text = formattedPrice // Tạm thời Phí ship = 0
    }

    private fun setupListeners() {
        binding.btnPlaceOrder.setOnClickListener {
            val name = binding.edtCustomerName.text.toString().trim()
            val phone = binding.edtCustomerPhone.text.toString().trim()
            val email = binding.edtCustomerEmail.text.toString().trim()
            val address = binding.edtCustomerAddress.text.toString().trim()
            val note = binding.edtNote.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc (*)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("CHECKOUT_UI", "Bắt đầu gọi ViewModel xử lý Checkout...")
            viewModel.processCheckout(name, phone, email, address, note)
        }
    }

    private fun observeViewModel() {
        // --- Lắng nghe trạng thái Loading ---
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarCheckout.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnPlaceOrder.isEnabled = !isLoading
        }

        // --- Lắng nghe Lỗi ---
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Log.e("CHECKOUT_UI", "Lỗi từ ViewModel: $errorMsg")
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        // --- DUY NHẤT 1 LẦN Lắng nghe Link VNPay trả về (Luồng WebView) ---
        viewModel.paymentUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                Log.d("CHECKOUT_UI", "Nhận được URL VNPay: $url")

                // Lấy mã Đơn hàng mà ViewModel vừa lưu lại
                val currentOrderId = viewModel.createdOrderId.value ?: -1

                // CHUYỂN HƯỚNG SANG MÀN HÌNH WEBVIEW CỦA CHÚNG TA
                val intent = Intent(this, PaymentWebViewActivity::class.java)
                intent.putExtra("EXTRA_PAYMENT_URL", url)
                intent.putExtra("EXTRA_ORDER_ID", currentOrderId)
                startActivity(intent)

                // Đóng màn hình Checkout lại
                finish()
            }
        }
    }
}