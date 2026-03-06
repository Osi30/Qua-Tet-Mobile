package com.semester7.quatet.data.remote

import com.semester7.quatet.data.model.BaseResponse
import com.semester7.quatet.data.model.ChatConversationDTO
import com.semester7.quatet.data.model.ChatMessageDTO
import com.semester7.quatet.data.model.SendChatMessageRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ChatApiService {

    @GET("api/chat/conversation")
    suspend fun getMyConversation(): BaseResponse<ChatConversationDTO?>

    @GET("api/chat/messages/me/{conversationId}")
    suspend fun getMessages(@Path("conversationId") conversationId: Int): BaseResponse<List<ChatMessageDTO>>

    @POST("api/chat/send")
    suspend fun sendMessage(@Body request: SendChatMessageRequest): BaseResponse<ChatMessageDTO>

    @PUT("api/chat/read/{conversationId}")
    suspend fun markRead(@Path("conversationId") conversationId: Int): BaseResponse<String?>
}

