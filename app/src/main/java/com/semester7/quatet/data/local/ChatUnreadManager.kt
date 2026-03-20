package com.semester7.quatet.data.local

import android.content.Context
import com.semester7.quatet.data.repository.ChatRepository

object ChatUnreadManager {
    private const val PREF_NAME = "QuaTetSession"
    private const val KEY_CHAT_UNREAD = "chat_unread"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun hasUnread(context: Context): Boolean {
        return prefs(context.applicationContext).getBoolean(KEY_CHAT_UNREAD, false)
    }

    fun setUnread(context: Context, value: Boolean) {
        prefs(context.applicationContext).edit().putBoolean(KEY_CHAT_UNREAD, value).apply()
    }

    suspend fun refreshFromServer(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!SessionManager.isLoggedIn(appContext)) {
            setUnread(appContext, false)
            return false
        }

        return try {
            val repository = ChatRepository()
            val currentUserId = SessionManager.getAccountId(appContext)
            val conversation = repository.getConversation()
            val unread = if (conversation == null) {
                false
            } else {
                val inlineMessages = conversation.messages
                if (inlineMessages.isNotEmpty()) {
                    inlineMessages.any { message -> !message.isRead && message.senderId != currentUserId }
                } else {
                    repository.getMessages(conversation.id)
                        .any { message -> !message.isRead && message.senderId != currentUserId }
                }
            }
            setUnread(appContext, unread)
            unread
        } catch (_: Exception) {
            hasUnread(appContext)
        }
    }
}
