package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.databinding.ActivitySettingsBinding
import com.semester7.quatet.utils.NotificationHelper

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BottomTabNavigator.setup(this, BottomTabNavigator.Tab.USER)
        binding.itemAddress.setOnClickListener {
            startActivity(Intent(this, AddressActivity::class.java))
        }
        binding.itemChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        binding.itemLogout.setOnClickListener {
            showLogoutConfirm()
        }
    }

    private fun showLogoutConfirm() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có muốn đăng xuất không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                SessionManager.clearSession(this)
                NotificationHelper.clearBadge(this)
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
