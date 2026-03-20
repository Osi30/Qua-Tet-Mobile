package com.semester7.quatet.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semester7.quatet.data.model.ChatMessageDTO
import com.semester7.quatet.data.model.StaffConversationItem
import com.semester7.quatet.data.remote.ConversationUpdatedEvent
import com.semester7.quatet.data.repository.ChatRepository
import kotlinx.coroutines.launch

class StaffChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private var currentUserId: Int = -1

    private val _conversations = MutableLiveData<List<StaffConversationItem>>(emptyList())
    val conversations: LiveData<List<StaffConversationItem>> = _conversations

    private val _selectedConversationId = MutableLiveData<Int?>()
    val selectedConversationId: LiveData<Int?> = _selectedConversationId

    private val _messages = MutableLiveData<List<ChatMessageDTO>>(emptyList())
    val messages: LiveData<List<ChatMessageDTO>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSending = MutableLiveData(false)
    val isSending: LiveData<Boolean> = _isSending

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun setCurrentUserId(userId: Int) {
        currentUserId = userId
    }

    fun loadConversations() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val baseItems = repository.getAllConversations().map { conversation ->
                    StaffConversationItem(
                        id = conversation.id,
                        userId = conversation.userId,
                        createdAt = conversation.createdAt,
                        lastMessageAt = conversation.lastMessageAt,
                        hasUnread = false
                    )
                }
                val mapped = if (currentUserId > 0) {
                    baseItems.map { item ->
                        val messages = repository.getMessagesForStaff(item.id)
                        val latestMessage = messages.maxByOrNull { it.id }
                        val hasUnread = messages.any { message ->
                            !message.isRead && message.senderId != currentUserId
                        }
                        item.copy(
                            lastMessageAt = latestMessage?.createdAt ?: item.lastMessageAt,
                            lastMessage = latestMessage?.content ?: item.lastMessage,
                            hasUnread = hasUnread
                        )
                    }
                } else {
                    baseItems
                }

                _conversations.value = sortConversations(mapped)

                val selectedId = _selectedConversationId.value
                if (selectedId != null && mapped.none { it.id == selectedId }) {
                    _selectedConversationId.value = null
                    _messages.value = emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Khong the tai danh sach chat"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectConversation(conversationId: Int) {
        _selectedConversationId.value = conversationId
        clearConversationUnread(conversationId)
        loadMessages(conversationId, silent = false, markRead = true)
    }

    fun refreshSelectedConversationMessages(silent: Boolean = true) {
        val selectedId = _selectedConversationId.value ?: return
        loadMessages(selectedId, silent = silent, markRead = true)
    }

    private fun loadMessages(conversationId: Int, silent: Boolean, markRead: Boolean) {
        if (!silent) _isLoading.value = true
        viewModelScope.launch {
            try {
                _messages.value = repository.getMessagesForStaff(conversationId).sortedBy { it.id }
                if (markRead) {
                    repository.markRead(conversationId)
                    clearConversationUnread(conversationId)
                }
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
        val selectedId = _selectedConversationId.value
        if (selectedId == null) {
            _errorMessage.value = "Hay chon 1 conversation truoc"
            return
        }

        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

        _isSending.value = true
        viewModelScope.launch {
            try {
                val sent = repository.replyToConversation(selectedId, trimmed)
                upsertMessage(sent)
                applyConversationMessage(sent, sent.senderId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Khong the gui tin nhan"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun applyIncomingMessage(message: ChatMessageDTO, currentUserId: Int) {
        val selectedId = _selectedConversationId.value
        if (selectedId == message.conversationId) {
            upsertMessage(message)
            viewModelScope.launch {
                runCatching { repository.markRead(message.conversationId) }
            }
        }
        applyConversationMessage(message, currentUserId)
    }

    fun applyConversationUpdated(event: ConversationUpdatedEvent, currentUserId: Int) {
        val selectedId = _selectedConversationId.value
        val isSelected = selectedId == event.conversationId
        val shouldUnread = event.hasNewMessage &&
            event.lastSenderId != null &&
            event.lastSenderId != currentUserId &&
            !isSelected

        val current = _conversations.value.orEmpty().toMutableList()
        val idx = current.indexOfFirst { it.id == event.conversationId }

        if (idx >= 0) {
            val old = current[idx]
            current[idx] = old.copy(
                userId = event.userId ?: old.userId,
                lastMessageAt = event.lastMessageAt ?: old.lastMessageAt,
                lastMessage = event.lastMessage ?: old.lastMessage,
                hasUnread = if (isSelected) false else (old.hasUnread || shouldUnread)
            )
        } else {
            current.add(
                StaffConversationItem(
                    id = event.conversationId,
                    userId = event.userId,
                    lastMessageAt = event.lastMessageAt,
                    lastMessage = event.lastMessage,
                    hasUnread = shouldUnread
                )
            )
        }

        _conversations.value = sortConversations(current)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun markConversationOpened(conversationId: Int) {
        clearConversationUnread(conversationId)
    }

    private fun clearConversationUnread(conversationId: Int) {
        _conversations.value = _conversations.value.orEmpty().map { item ->
            if (item.id == conversationId) item.copy(hasUnread = false) else item
        }
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

    private fun applyConversationMessage(message: ChatMessageDTO, currentUserId: Int) {
        val selectedId = _selectedConversationId.value
        val current = _conversations.value.orEmpty().toMutableList()
        val idx = current.indexOfFirst { it.id == message.conversationId }
        val shouldUnread = message.senderId != currentUserId && selectedId != message.conversationId

        if (idx >= 0) {
            val old = current[idx]
            current[idx] = old.copy(
                lastMessageAt = message.createdAt ?: old.lastMessageAt,
                lastMessage = message.content,
                hasUnread = if (selectedId == message.conversationId) false else (old.hasUnread || shouldUnread)
            )
        } else {
            current.add(
                StaffConversationItem(
                    id = message.conversationId,
                    lastMessageAt = message.createdAt,
                    lastMessage = message.content,
                    hasUnread = shouldUnread
                )
            )
        }

        _conversations.value = sortConversations(current)
    }

    private fun sortConversations(items: List<StaffConversationItem>): List<StaffConversationItem> {
        return items.sortedWith(
            compareByDescending<StaffConversationItem> { it.lastMessageAt ?: it.createdAt ?: "" }
                .thenByDescending { it.id }
        )
    }
}
