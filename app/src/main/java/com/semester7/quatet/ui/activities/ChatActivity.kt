package com.semester7.quatet.ui.activities

import android.os.Handler
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semester7.quatet.data.local.ChatUnreadManager
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.data.remote.RetrofitClient
import com.semester7.quatet.data.remote.TypingEvent
import com.semester7.quatet.data.remote.ChatSignalRClient
import com.semester7.quatet.databinding.ActivityChatBinding
import com.semester7.quatet.ui.adapters.ChatMessageAdapter
import com.semester7.quatet.viewmodel.ChatViewModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatMessageAdapter
    private lateinit var signalRClient: ChatSignalRClient
    private var currentUserId: Int = -1
    private val typingHandler = Handler(Looper.getMainLooper())
    private var stopTypingRunnable: Runnable? = null
    private var isTypingActive = false

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
        ChatUnreadManager.setUnread(this, false)

        viewModel.loadConversationAndMessages()
    }

    private fun setupRecyclerView() {
        currentUserId = SessionManager.getAccountId(this)
        adapter = ChatMessageAdapter(currentUserId = currentUserId)

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
            stopTyping()
        }

        binding.edtMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                handleTypingInput(s?.toString().orEmpty())
            }
        })
    }

    private fun handleTypingInput(text: String) {
        val conversationId = viewModel.conversationId.value ?: return
        if (text.isBlank()) {
            stopTyping()
            return
        }

        if (!isTypingActive) {
            signalRClient.sendTyping(conversationId, true)
            isTypingActive = true
        }

        stopTypingRunnable?.let { typingHandler.removeCallbacks(it) }
        stopTypingRunnable = Runnable {
            if (isTypingActive) {
                signalRClient.sendTyping(conversationId, false)
                isTypingActive = false
            }
        }.also {
            typingHandler.postDelayed(it, 1_500L)
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
                binding.tvTypingIndicator.visibility = View.GONE
                signalRClient.switchConversation(conversationId)
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
            onReceiveMessage = { message ->
                runOnUiThread {
                    viewModel.handleIncomingMessage(message)
                    if (message.senderId != currentUserId) {
                        viewModel.markConversationRead()
                        ChatUnreadManager.setUnread(this, false)
                        binding.tvTypingIndicator.visibility = View.GONE
                    }
                }
            },
            onTypingChanged = { event ->
                runOnUiThread { handleTypingEvent(event) }
            },
            onMessagesRead = { readEvent ->
                runOnUiThread {
                    val currentConversationId = viewModel.conversationId.value
                    if (currentConversationId == readEvent.conversationId) {
                        viewModel.handleMessagesRead(readEvent.messageIds)
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

    private fun handleTypingEvent(event: TypingEvent) {
        val currentConversationId = viewModel.conversationId.value ?: return
        if (event.conversationId != currentConversationId) return
        if (event.userId == currentUserId) return

        binding.tvTypingIndicator.visibility = if (event.isTyping) View.VISIBLE else View.GONE
    }

    private fun stopTyping() {
        stopTypingRunnable?.let { typingHandler.removeCallbacks(it) }
        stopTypingRunnable = null
        if (!isTypingActive) return
        val conversationId = viewModel.conversationId.value ?: return
        signalRClient.sendTyping(conversationId, false)
        isTypingActive = false
    }

    private fun resetTypingUi() {
        stopTypingRunnable?.let { typingHandler.removeCallbacks(it) }
        stopTypingRunnable = null
        isTypingActive = false
        binding.tvTypingIndicator.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        ChatUnreadManager.setUnread(this, false)
        signalRClient.connect()
        viewModel.conversationId.value?.let { conversationId ->
            signalRClient.switchConversation(conversationId)
        }
    }

    override fun onStop() {
        super.onStop()
        stopTyping()
        resetTypingUi()
        signalRClient.disconnect()
    }
}
