package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.semester7.quatet.data.local.ChatUnreadManager
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.databinding.ActivitySettingsBinding
import com.semester7.quatet.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private companion object {
        private const val CHAT_BADGE_SYNC_INTERVAL_MS = 1_000L
    }

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BottomTabNavigator.setup(this, BottomTabNavigator.Tab.USER)
        renderChatBadge(ChatUnreadManager.hasUnread(this))
        startChatBadgeSyncLoop()

        binding.itemAddress.setOnClickListener {
            startActivity(Intent(this, AddressActivity::class.java))
        }
        binding.itemChat.setOnClickListener {
            BottomTabNavigator.setChatUnread(this, false)
            renderChatBadge(false)
            startActivity(Intent(this, ChatActivity::class.java))
        }
        binding.itemLogout.setOnClickListener {
            showLogoutConfirm()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshChatBadge()
    }

    private fun refreshChatBadge() {
        val cached = ChatUnreadManager.hasUnread(this)
        renderChatBadge(cached)
        BottomTabNavigator.renderChatBadge(this, cached)

        lifecycleScope.launch {
            val unread = withContext(Dispatchers.IO) {
                ChatUnreadManager.refreshFromServer(applicationContext)
            }
            renderChatBadge(unread)
            BottomTabNavigator.renderChatBadge(this@SettingsActivity, unread)
        }
    }

    private fun renderChatBadge(hasUnread: Boolean) {
        binding.viewChatBadge.visibility = if (hasUnread) View.VISIBLE else View.GONE
    }

    private fun startChatBadgeSyncLoop() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    renderChatBadge(ChatUnreadManager.hasUnread(this@SettingsActivity))
                    delay(CHAT_BADGE_SYNC_INTERVAL_MS)
                }
            }
        }
    }

    private fun showLogoutConfirm() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có muốn đăng xuất không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                SessionManager.clearSession(this)
                ChatUnreadManager.setUnread(this, false)
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
