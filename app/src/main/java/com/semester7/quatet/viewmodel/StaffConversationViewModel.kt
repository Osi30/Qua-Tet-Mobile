package com.semester7.quatet.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.ChatMessageDTO
import com.semester7.quatet.data.repository.ChatRepository
import kotlinx.coroutines.launch

class StaffConversationViewModel : ViewModel() {

    private val repository = ChatRepository()

    private var conversationId: Int = -1
    private var currentUserId: Int = -1
    private var initialized = false

    private val _messages = MutableLiveData<List<ChatMessageDTO>>(emptyList())
    val messages: LiveData<List<ChatMessageDTO>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSending = MutableLiveData(false)
    val isSending: LiveData<Boolean> = _isSending

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun init(conversationId: Int, currentUserId: Int) {
        if (initialized && this.conversationId == conversationId) return
        this.conversationId = conversationId
        this.currentUserId = currentUserId
        initialized = true
        loadMessages(silent = false)
    }

    fun loadMessages(silent: Boolean = true) {
        if (conversationId <= 0) return
        if (!silent) _isLoading.value = true

        viewModelScope.launch {
            try {
                val loaded = repository.getMessagesForStaff(conversationId).sortedBy { it.id }
                _messages.value = loaded
                repository.markRead(conversationId)
                markIncomingMessagesAsRead()
            } catch (e: Exception) {
                if (!silent) {
                    _errorMessage.value = e.message ?: "Khong the tai tin nhan"
                }
            } finally {
                if (!silent) _isLoading.value = false
            }
        }
    }

    fun sendReply(content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty() || conversationId <= 0) return

        _isSending.value = true
        viewModelScope.launch {
            try {
                val sent = repository.replyToConversation(conversationId, trimmed)
                upsertMessage(sent)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Khong the gui tin nhan"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun applyIncomingMessage(message: ChatMessageDTO) {
        if (message.conversationId != conversationId) return

        upsertMessage(message)
        if (message.senderId != currentUserId) {
            viewModelScope.launch {
                runCatching { repository.markRead(conversationId) }
            }
            markIncomingMessagesAsRead()
        }
    }

    fun handleMessagesRead(messageIds: List<Int>) {
        if (messageIds.isEmpty()) return
        val idSet = messageIds.toSet()
        _messages.value = _messages.value.orEmpty().map { message ->
            if (idSet.contains(message.id)) message.copy(isRead = true) else message
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun upsertMessage(message: ChatMessageDTO) {
        val current = _messages.value.orEmpty().toMutableList()
        val idx = current.indexOfFirst { it.id == message.id }
        if (idx >= 0) {
            current[idx] = message
        } else {
            current.add(message)
        }
        _messages.value = current.sortedBy { it.id }
    }

    private fun markIncomingMessagesAsRead() {
        _messages.value = _messages.value.orEmpty().map { message ->
            if (message.senderId != currentUserId && !message.isRead) {
                message.copy(isRead = true)
            } else {
                message
            }
        }
    }
}
