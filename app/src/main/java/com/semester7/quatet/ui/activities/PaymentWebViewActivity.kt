package com.semester7.quatet.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    // Biến cờ để tránh việc xử lý URL nhiều lần nếu onPageFinished bị gọi lặp
    private var isResultHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Nhận Link và OrderId từ màn hình Checkout truyền sang
        val paymentUrl = intent.getStringExtra("EXTRA_PAYMENT_URL")
        currentOrderId = intent.getIntExtra("EXTRA_ORDER_ID", -1)

        Log.d("WEBVIEW_PAYMENT", "Nhận được URL thanh toán: $paymentUrl")
        Log.d("WEBVIEW_PAYMENT", "Mã đơn hàng hiện tại: $currentOrderId")

        // Kiểm tra an toàn, nếu thiếu data thì đóng luôn
        if (paymentUrl.isNullOrEmpty() || currentOrderId == -1) {
            Log.e("WEBVIEW_PAYMENT", "LỖI: Dữ liệu truyền sang bị thiếu!")
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
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        binding.webView.webViewClient = object : WebViewClient() {

            // Sự kiện 1: Khi bắt đầu tải một trang
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("WEBVIEW_PAYMENT", "BẮT ĐẦU tải trang: $url")
                binding.progressBar.visibility = View.VISIBLE // Hiện vòng xoay
            }

            // Sự kiện 2 (CỐT LÕI MỚI): Bắt mọi cú click và chuyển hướng URL
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d("WEBVIEW_PAYMENT", "CHUYỂN HƯỚNG tới URL: $url")

                // TRẢ VỀ FALSE CỰC KỲ QUAN TRỌNG!
                // Để WebView tiếp tục load trang này, giúp Backend của bạn nhận được tín hiệu từ VNPay.
                return false
            }

            // Sự kiện 3: Khi tải xong trang
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WEBVIEW_PAYMENT", "TẢI XONG trang: $url")
                binding.progressBar.visibility = View.GONE // Ẩn vòng xoay

                // Chỉ xử lý khi URL có chứa mã phản hồi VÀ chưa được xử lý trước đó
                if (url != null && url.contains("vnp_ResponseCode") && !isResultHandled) {
                    isResultHandled = true // Đánh dấu là đã xử lý để không bị gọi đúp
                    Log.d("WEBVIEW_PAYMENT", "🔥 Đã phát hiện URL Return của VNPay!")

                    // Vô hiệu hóa webview để user không bấm bậy bạ được nữa
                    binding.webView.isEnabled = false

                    Log.d("WEBVIEW_PAYMENT", "⏳ Đang đếm ngược 2 giây chờ Server lưu Database...")
                    // Nghỉ 2 giây để chắc chắn Backend đã xử lý xong trước khi App nhảy sang màn Billing
                    Handler(Looper.getMainLooper()).postDelayed({
                        handleVNPayResult(url)
                    }, 2000)
                }
            }
        }
    }

    private fun handleVNPayResult(url: String) {
        Log.d("WEBVIEW_PAYMENT", "Tiến hành phân tích mã kết quả...")

        // VNPay quy định: vnp_ResponseCode=00 là Giao dịch thành công
        if (url.contains("vnp_ResponseCode=00")) {
            Log.d("WEBVIEW_PAYMENT", "✅ Kết quả: THÀNH CÔNG (Mã 00)")
            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show()

            // CHUYỂN THẲNG SANG MÀN HÌNH HÓA ĐƠN
            val intent = Intent(this, BillingActivity::class.java)
            intent.putExtra("EXTRA_ORDER_ID", currentOrderId)
            startActivity(intent)

        } else {
            // Các mã khác (như 24: Khách hàng hủy thanh toán, 51: Không đủ tiền...)
            Log.e("WEBVIEW_PAYMENT", "❌ Kết quả: THẤT BẠI HOẶC HỦY GIAO DỊCH")
            Toast.makeText(this, "Giao dịch thất bại hoặc bị hủy!", Toast.LENGTH_LONG).show()
        }

        Log.d("WEBVIEW_PAYMENT", "Đóng màn hình WebView.")
        finish()
    }
}