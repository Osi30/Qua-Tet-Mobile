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

        if (SessionManager.isLoggedIn(this)) {
            navigateByRole(SessionManager.getRole(this))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showError("Vui long nhap day du ten dang nhap va mat khau")
                return@setOnClickListener
            }

            viewModel.login(username, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
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

            Toast.makeText(this, "Dang nhap thanh cong!", Toast.LENGTH_SHORT).show()
            navigateByRole(result.role)
            setResult(RESULT_OK)
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
