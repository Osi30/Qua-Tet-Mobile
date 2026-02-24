package com.semester7.quatet.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.databinding.ActivityPaymentWebViewBinding

class PaymentWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentWebViewBinding
    private var currentOrderId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Nhận Link và OrderId từ màn hình Checkout truyền sang
        val paymentUrl = intent.getStringExtra("EXTRA_PAYMENT_URL")
        currentOrderId = intent.getIntExtra("EXTRA_ORDER_ID", -1)

        // Kiểm tra an toàn, nếu thiếu data thì đóng luôn
        if (paymentUrl.isNullOrEmpty() || currentOrderId == -1) {
            Toast.makeText(this, "Dữ liệu thanh toán không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Cài đặt các thông số cho khung duyệt Web
        setupWebView()

        // 3. Bắt đầu tải trang VNPay
        binding.webView.loadUrl(paymentUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = binding.webView.settings
        // RẤT QUAN TRỌNG: Bắt buộc phải bật JavaScript thì cổng VNPay mới hoạt động được
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // Cài đặt "Điệp viên" theo dõi WebView
        binding.webView.webViewClient = object : WebViewClient() {

            // Sự kiện 1: Khi bắt đầu tải một trang
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE // Hiện vòng xoay
            }

            // Sự kiện 2: Khi tải xong trang
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE // Ẩn vòng xoay
            }

            // Sự kiện 3 (Cốt lõi): Bắt mọi cú click và chuyển hướng URL
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d("WEBVIEW_PAYMENT", "Đang chuyển hướng tới URL: $url")

                // Nếu URL có chứa mã phản hồi của VNPay, ta sẽ tóm lấy nó!
                if (url.contains("vnp_ResponseCode")) {
                    handleVNPayResult(url)
                    return true // Trả về true báo cho App biết: "Tôi đã xử lý link này, không cần load web nữa"
                }

                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    }

    private fun handleVNPayResult(url: String) {
        // VNPay quy định: vnp_ResponseCode=00 là Giao dịch thành công
        if (url.contains("vnp_ResponseCode=00")) {
            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show()

            // CHUYỂN THẲNG SANG MÀN HÌNH HÓA ĐƠN
            val intent = Intent(this, BillingActivity::class.java)
            intent.putExtra("EXTRA_ORDER_ID", currentOrderId)
            startActivity(intent)

        } else {
            // Các mã khác (như 24: Khách hàng hủy thanh toán, 51: Không đủ tiền...)
            Toast.makeText(this, "Giao dịch thất bại hoặc bị hủy!", Toast.LENGTH_LONG).show()
        }

        // Dù thành công hay thất bại thì cũng đóng màn hình WebView này lại (Không cho user back lại trang thanh toán nữa)
        finish()
    }
}