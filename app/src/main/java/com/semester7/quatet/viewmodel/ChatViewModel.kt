package com.semester7.quatet.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.ChatMessageDTO
import com.semester7.quatet.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _conversationId = MutableLiveData<Int?>()
    val conversationId: LiveData<Int?> = _conversationId

    private val _messages = MutableLiveData<List<ChatMessageDTO>>(emptyList())
    val messages: LiveData<List<ChatMessageDTO>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSending = MutableLiveData(false)
    val isSending: LiveData<Boolean> = _isSending

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadConversationAndMessages() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val conversation = repository.getConversation()
                _conversationId.value = conversation?.id
                if (conversation != null) {
                    _messages.value = repository.getMessages(conversation.id)
                    repository.markRead(conversation.id)
                } else {
                    _messages.value = emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Khong the tai cuoc tro chuyen"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshMessages(silent: Boolean = true) {
        val id = _conversationId.value ?: return
        if (!silent) _isLoading.value = true
        viewModelScope.launch {
            try {
                _messages.value = repository.getMessages(id)
                repository.markRead(id)
            } catch (e: Exception) {
                if (!silent) {
                    _errorMessage.value = e.message ?: "Không thể tải tin nhan"
                }
            } finally {
                if (!silent) _isLoading.value = false
            }
        }
    }

    fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

        _isSending.value = true
        viewModelScope.launch {
            try {
                val sent = repository.sendMessage(trimmed, orderId = null)
                if (_conversationId.value == null) _conversationId.value = sent.conversationId
                handleIncomingMessage(sent)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Khong the gui tin nhan"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun handleIncomingMessage(message: ChatMessageDTO) {
        if (_conversationId.value == null) {
            _conversationId.value = message.conversationId
        }
        if (_conversationId.value != message.conversationId) return

        val current = _messages.value.orEmpty().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == message.id }
        if (existingIndex >= 0) {
            current[existingIndex] = message
        } else {
            current.add(message)
        }
        _messages.value = current.sortedBy { it.id }
    }

    fun handleMessagesRead(messageIds: List<Int>) {
        if (messageIds.isEmpty()) return
        val idSet = messageIds.toSet()
        _messages.value = _messages.value.orEmpty().map { message ->
            if (idSet.contains(message.id)) message.copy(isRead = true) else message
        }
    }

    fun markConversationRead() {
        val id = _conversationId.value ?: return
        viewModelScope.launch {
            try {
                repository.markRead(id)
            } catch (_: Exception) {
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
    }
}
