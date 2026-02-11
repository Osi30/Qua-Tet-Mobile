package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.databinding.ActivityRegisterBinding
import com.semester7.quatet.viewmodel.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        // Nút Back
        binding.btnBack.setOnClickListener { finish() }

        // Nút Đăng ký
        binding.btnRegister.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val fullname = binding.edtFullname.text.toString().trim().ifEmpty { null }
            val phone = binding.edtPhone.text.toString().trim().ifEmpty { null }

            // Validate input (3 trường bắt buộc)
            if (username.isEmpty()) {
                showError("Vui lòng nhập tên đăng nhập")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError("Vui lòng nhập mật khẩu")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showError("Mật khẩu phải có ít nhất 6 ký tự")
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                showError("Vui lòng nhập email để nhận mã OTP")
                return@setOnClickListener
            }

            viewModel.register(username, password, email, fullname, phone)
        }

        // Link Đăng nhập
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        // Loading
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
        }

        // Register thành công → chuyển sang OTP
        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                val username = binding.edtUsername.text.toString().trim()
                val intent = Intent(this, OtpActivity::class.java)
                intent.putExtra("username", username)
                startActivity(intent)
                finish()
            }
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
