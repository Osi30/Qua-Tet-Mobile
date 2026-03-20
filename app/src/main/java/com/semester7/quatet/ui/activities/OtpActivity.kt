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

        username = intent.getStringExtra("username") ?: ""

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnVerify.setOnClickListener {
            val otp = binding.edtOtp.text.toString().trim()
            if (otp.length != 6) {
                showError("Vui long nhap dung OTP 6 so")
                return@setOnClickListener
            }
            viewModel.verifyOtp(username, otp)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnVerify.isEnabled = !isLoading
        }

        viewModel.authResult.observe(this) { result ->
            SessionManager.saveSession(
                context = this,
                token = result.token,
                accountId = result.accountId,
                username = result.username,
                email = result.email,
                role = result.role
            )

            Toast.makeText(this, "Dang ky thanh cong!", Toast.LENGTH_SHORT).show()
            navigateByRole(result.role)
            finish()
        }

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

    private fun navigateByRole(role: String?) {
        val target = if (isStaffOrAdmin(role)) StaffChatActivity::class.java else ProductActivity::class.java
        val intent = Intent(this, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun isStaffOrAdmin(role: String?): Boolean {
        val normalized = role?.uppercase().orEmpty()
        return normalized == "STAFF" || normalized == "ADMIN"
    }
}
