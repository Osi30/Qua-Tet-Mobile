package com.semester7.quatet.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.data.remote.RetrofitClient
import com.semester7.quatet.data.remote.ChatSignalRClient
import com.semester7.quatet.databinding.ActivityChatBinding
import com.semester7.quatet.ui.adapters.ChatMessageAdapter
import com.semester7.quatet.viewmodel.ChatViewModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatMessageAdapter
    private lateinit var signalRClient: ChatSignalRClient
    private var joinedConversationId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        signalRClient = ChatSignalRClient(this)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        setupSignalR()

        viewModel.loadConversationAndMessages()
    }

    private fun setupRecyclerView() {
        val accountId = SessionManager.getAccountId(this)
        adapter = ChatMessageAdapter(currentUserId = accountId)

        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnSend.setOnClickListener {
            val content = binding.edtMessage.text?.toString().orEmpty()
            if (content.isBlank()) return@setOnClickListener
            viewModel.sendMessage(content)
            binding.edtMessage.setText("")
            val conversationId = viewModel.conversationId.value
            if (conversationId != null) {
                signalRClient.sendTyping(conversationId, false)
            }
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
            adapter.updateData(messages)
            binding.layoutEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
            if (messages.isNotEmpty()) {
                binding.rvMessages.scrollToPosition(messages.lastIndex)
            }
        }

        viewModel.conversationId.observe(this) { conversationId ->
            if (conversationId != null) {
                connectAndJoinConversation(conversationId)
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun setupSignalR() {
        signalRClient.setCallbacks(
            onReceiveMessage = {
                runOnUiThread {
                    viewModel.refreshMessages(silent = true)
                }
            },
            onTypingChanged = { _ -> },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun connectAndJoinConversation(conversationId: Int) {
        if (joinedConversationId == conversationId) return

        val previousId = joinedConversationId
        signalRClient.connect {
            if (previousId != null) {
                signalRClient.leaveConversation(previousId)
            }
            signalRClient.joinConversation(conversationId)
            joinedConversationId = conversationId
        }
    }

    override fun onStart() {
        super.onStart()
        val conversationId = viewModel.conversationId.value
        if (conversationId != null) {
            connectAndJoinConversation(conversationId)
        }
    }

    override fun onStop() {
        super.onStop()
        joinedConversationId?.let { signalRClient.leaveConversation(it) }
        signalRClient.disconnect()
        joinedConversationId = null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
