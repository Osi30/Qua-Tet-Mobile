package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.databinding.ActivityLoginBinding
import com.semester7.quatet.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        // Nút Đăng nhập
        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            // Validate input
            if (username.isEmpty() || password.isEmpty()) {
                showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu")
                return@setOnClickListener
            }

            viewModel.login(username, password)
        }

        // Link Đăng ký
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeViewModel() {
        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
        }

        // Login thành công
        viewModel.authResult.observe(this) { result ->
            // Lưu session
            SessionManager.saveSession(
                context = this,
                token = result.token,
                accountId = result.accountId,
                username = result.username,
                email = result.email,
                role = result.role
            )

            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

            // Quay lại trang trước
            setResult(RESULT_OK)
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
