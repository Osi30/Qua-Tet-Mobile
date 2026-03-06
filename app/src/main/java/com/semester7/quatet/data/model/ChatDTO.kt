package com.semester7.quatet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatConversationDTO(
    val id: Int,
    val userId: Int? = null,
    val createdAt: String? = null,
    val lastMessageAt: String? = null,
    val messages: List<ChatMessageDTO> = emptyList()
)

@Serializable
data class ChatMessageDTO(
    val id: Int,
    val conversationId: Int,
    val senderId: Int,
    val orderId: Int? = null,
    val content: String,
    val isRead: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class SendChatMessageRequest(
    val orderId: Int? = null,
    val content: String
)

