package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semester7.quatet.data.local.ChatUnreadManager
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.data.remote.ChatSignalRClient
import com.semester7.quatet.data.model.StaffConversationItem
import com.semester7.quatet.databinding.ActivityStaffChatBinding
import com.semester7.quatet.ui.adapters.StaffConversationAdapter
import com.semester7.quatet.viewmodel.StaffChatViewModel

class StaffChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffChatBinding
    private val viewModel: StaffChatViewModel by viewModels()
    private lateinit var conversationAdapter: StaffConversationAdapter
    private lateinit var signalRClient: ChatSignalRClient

    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isStaffOrAdmin()) {
            startActivity(Intent(this, ProductActivity::class.java))
            finish()
            return
        }

        currentUserId = SessionManager.getAccountId(this)
        viewModel.setCurrentUserId(currentUserId)
        signalRClient = ChatSignalRClient(this)

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        setupSignalR()
    }

    private fun setupRecyclerViews() {
        conversationAdapter = StaffConversationAdapter { item ->
            openConversation(item)
        }
        binding.rvConversations.layoutManager = LinearLayoutManager(this)
        binding.rvConversations.adapter = conversationAdapter
    }

    private fun setupListeners() {
        binding.tvLogout.setOnClickListener {
            showLogoutConfirm()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.conversations.observe(this) { conversations ->
            conversationAdapter.updateData(conversations, null)
            binding.tvConversationEmpty.visibility = if (conversations.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupSignalR() {
        signalRClient.setCallbacks(
            onReceiveMessage = { message ->
                runOnUiThread {
                    viewModel.applyIncomingMessage(message, currentUserId)
                }
            },
            onConversationUpdated = { event ->
                runOnUiThread {
                    viewModel.applyConversationUpdated(event, currentUserId)
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        signalRClient.connect()
        viewModel.loadConversations()
    }

    override fun onStop() {
        super.onStop()
        signalRClient.disconnect()
    }

    private fun openConversation(item: StaffConversationItem) {
        viewModel.markConversationOpened(item.id)
        val intent = Intent(this, StaffConversationActivity::class.java).apply {
            putExtra(StaffConversationActivity.EXTRA_CONVERSATION_ID, item.id)
            putExtra(StaffConversationActivity.EXTRA_USER_ID, item.userId ?: -1)
        }
        startActivity(intent)
    }

    private fun showLogoutConfirm() {
        AlertDialog.Builder(this)
            .setTitle("Dang xuat")
            .setMessage("Ban co muon dang xuat khong?")
            .setPositiveButton("Dang xuat") { _, _ ->
                SessionManager.clearSession(this)
                ChatUnreadManager.setUnread(this, false)
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Huy", null)
            .show()
    }

    private fun isStaffOrAdmin(): Boolean {
        val role = SessionManager.getRole(this)?.uppercase().orEmpty()
        return role == "STAFF" || role == "ADMIN"
    }
}
