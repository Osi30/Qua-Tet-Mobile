package com.semester7.quatet.data.repository

import com.semester7.quatet.data.model.ChatConversationDTO
import com.semester7.quatet.data.model.ChatMessageDTO
import com.semester7.quatet.data.model.SendChatMessageRequest
import com.semester7.quatet.data.remote.ChatApiService
import com.semester7.quatet.data.remote.RetrofitClient

class ChatRepository {
    private val apiService = RetrofitClient.createService(ChatApiService::class.java)

    suspend fun getConversation(): ChatConversationDTO? {
        return apiService.getMyConversation().data
    }

    suspend fun getMessages(conversationId: Int): List<ChatMessageDTO> {
        return apiService.getMessages(conversationId).data
    }

    suspend fun sendMessage(content: String, orderId: Int? = null): ChatMessageDTO {
        return apiService.sendMessage(SendChatMessageRequest(orderId = orderId, content = content)).data
    }

    suspend fun markRead(conversationId: Int) {
        val response = apiService.markRead(conversationId)
        if (!response.isSuccessful) {
            throw IllegalStateException("Khong the danh dau da doc (${response.code()})")
        }
    }
}

