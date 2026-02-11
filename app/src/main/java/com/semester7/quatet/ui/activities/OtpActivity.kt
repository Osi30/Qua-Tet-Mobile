package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.databinding.ActivityOtpBinding
import com.semester7.quatet.viewmodel.AuthViewModel

class OtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpBinding
    private val viewModel: AuthViewModel by viewModels()
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy username từ RegisterActivity
        username = intent.getStringExtra("username") ?: ""

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        // Nút Back
        binding.btnBack.setOnClickListener { finish() }

        // Nút Xác nhận OTP
        binding.btnVerify.setOnClickListener {
            val otp = binding.edtOtp.text.toString().trim()

            if (otp.isEmpty() || otp.length != 6) {
                showError("Vui lòng nhập đủ mã OTP 6 số")
                return@setOnClickListener
            }

            viewModel.verifyOtp(username, otp)
        }
    }

    private fun observeViewModel() {
        // Loading
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnVerify.isEnabled = !isLoading
        }

        // Xác thực thành công → lưu session → quay về ProductActivity
        viewModel.authResult.observe(this) { result ->
            SessionManager.saveSession(
                context = this,
                token = result.token,
                accountId = result.accountId,
                username = result.username,
                email = result.email,
                role = result.role
            )

            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()

            // Quay thẳng về ProductActivity, xóa LoginActivity khỏi back stack
            val intent = Intent(this, ProductActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Lỗi
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (errorMsg != null) {
                showError(errorMsg)
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}
