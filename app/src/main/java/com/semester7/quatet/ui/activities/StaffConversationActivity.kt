package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.data.remote.ChatSignalRClient
import com.semester7.quatet.databinding.ActivityStaffConversationBinding
import com.semester7.quatet.ui.adapters.ChatMessageAdapter
import com.semester7.quatet.viewmodel.StaffConversationViewModel

class StaffConversationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_USER_ID = "extra_user_id"
    }

    private lateinit var binding: ActivityStaffConversationBinding
    private val viewModel: StaffConversationViewModel by viewModels()
    private lateinit var messageAdapter: ChatMessageAdapter
    private lateinit var signalRClient: ChatSignalRClient

    private var currentUserId: Int = -1
    private var conversationId: Int = -1
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isStaffOrAdmin()) {
            startActivity(Intent(this, ProductActivity::class.java))
            finish()
            return
        }

        conversationId = intent.getIntExtra(EXTRA_CONVERSATION_ID, -1)
        userId = intent.getIntExtra(EXTRA_USER_ID, -1)
        if (conversationId <= 0) {
            finish()
            return
        }

        currentUserId = SessionManager.getAccountId(this)
        signalRClient = ChatSignalRClient(this)

        setupHeader()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        setupSignalR()

        viewModel.init(conversationId = conversationId, currentUserId = currentUserId)
    }

    private fun setupHeader() {
        binding.tvTitle.text = if (userId > 0) {
            "User #$userId"
        } else {
            "Conversation #$conversationId"
        }
        binding.tvSubtitle.text = "Conversation #$conversationId"
    }

    private fun setupRecyclerView() {
        messageAdapter = ChatMessageAdapter(currentUserId = currentUserId)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = messageAdapter
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }

        binding.btnSend.setOnClickListener {
            val content = binding.edtMessage.text?.toString().orEmpty()
            if (content.isBlank()) return@setOnClickListener
            viewModel.sendReply(content)
            binding.edtMessage.setText("")
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.isSending.observe(this) { sending ->
            binding.btnSend.isEnabled = !sending
        }

        viewModel.messages.observe(this) { messages ->
            messageAdapter.updateData(messages)
            binding.tvMessageEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
            if (messages.isNotEmpty()) {
                val lastPosition = messageAdapter.getLastAdapterPosition()
                if (lastPosition >= 0) {
                    binding.rvMessages.scrollToPosition(lastPosition)
                }
            }
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
                    viewModel.applyIncomingMessage(message)
                }
            },
            onMessagesRead = { event ->
                runOnUiThread {
                    if (event.conversationId == conversationId) {
                        viewModel.handleMessagesRead(event.messageIds)
                    }
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
        signalRClient.connect {
            signalRClient.switchConversation(conversationId)
        }
        viewModel.loadMessages(silent = true)
    }

    override fun onStop() {
        super.onStop()
        signalRClient.disconnect()
    }

    private fun isStaffOrAdmin(): Boolean {
        val role = SessionManager.getRole(this)?.uppercase().orEmpty()
        return role == "STAFF" || role == "ADMIN"
    }
}
