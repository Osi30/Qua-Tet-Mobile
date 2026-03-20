package com.semester7.quatet.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.semester7.quatet.data.model.ChatMessageDTO
import com.semester7.quatet.data.local.SessionManager
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min

class ChatSignalRClient(context: Context) {

    companion object {
        private const val HUB_URL = "http://14.225.207.221:5000/hubs/chat"
        private const val RECONNECT_BASE_DELAY_MS = 1_000L
        private const val RECONNECT_MAX_DELAY_MS = 30_000L
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hubConnection: HubConnection? = null
    private val pendingConnectedCallbacks = CopyOnWriteArrayList<() -> Unit>()

    private var shouldReconnect = false
    private var reconnectAttempt = 0
    private var reconnectRunnable: Runnable? = null
    private var activeConversationId: Int? = null

    private var onReceiveMessage: ((ChatMessageDTO) -> Unit)? = null
    private var onTypingChanged: ((TypingEvent) -> Unit)? = null
    private var onMessagesRead: ((MessagesReadEvent) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun setCallbacks(
        onReceiveMessage: ((ChatMessageDTO) -> Unit)? = null,
        onTypingChanged: ((TypingEvent) -> Unit)? = null,
        onMessagesRead: ((MessagesReadEvent) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        this.onReceiveMessage = onReceiveMessage
        this.onTypingChanged = onTypingChanged
        this.onMessagesRead = onMessagesRead
        this.onError = onError
    }

    fun connect(onConnected: (() -> Unit)? = null) {
        val token = SessionManager.getToken(appContext).orEmpty()
        if (token.isBlank()) {
            onError?.invoke("Chua dang nhap, khong the ket noi chat realtime.")
            return
        }

        shouldReconnect = true
        onConnected?.let { pendingConnectedCallbacks.add(it) }

        val connection = getOrCreateConnection()
        when (connection.connectionState) {
            HubConnectionState.CONNECTED -> notifyConnected()
            HubConnectionState.CONNECTING -> Unit
            else -> startConnection(connection)
        }
    }

    fun switchConversation(conversationId: Int) {
        val previousId = activeConversationId
        activeConversationId = conversationId

        connect {
            if (previousId != null && previousId != conversationId) {
                invokeServer("LeaveConversation", previousId)
            }
            invokeServer("JoinConversation", conversationId)
        }
    }

    fun sendTyping(conversationId: Int, isTyping: Boolean) {
        activeConversationId = conversationId
        invokeServer("SendTyping", conversationId, isTyping)
    }

    fun pingConversation(conversationId: Int) {
        activeConversationId = conversationId
        invokeServer("PingConversation", conversationId)
    }

    fun disconnect() {
        shouldReconnect = false
        cancelReconnect()
        pendingConnectedCallbacks.clear()

        val connection = hubConnection ?: return
        if (connection.connectionState != HubConnectionState.DISCONNECTED) {
            connection.stop().subscribe({}, {})
        }
        reconnectAttempt = 0
        hubConnection = null
    }

    private fun getOrCreateConnection(): HubConnection {
        val existing = hubConnection
        if (existing != null) return existing

        val created = HubConnectionBuilder.create(HUB_URL)
            .withAccessTokenProvider(Single.defer { Single.just(SessionManager.getToken(appContext).orEmpty()) })
            .build()
            .apply {
                on("ReceiveMessage", { payload ->
                    parseChatMessage(payload)?.let { onReceiveMessage?.invoke(it) }
                }, Any::class.java)

                on("UserTyping", { payload ->
                    parseTypingEvent(payload)?.let { onTypingChanged?.invoke(it) }
                }, Any::class.java)

                on("MessagesRead", { payload ->
                    parseMessagesReadEvent(payload)?.let { onMessagesRead?.invoke(it) }
                }, Any::class.java)

                onClosed { error ->
                    if (error != null) {
                        onError?.invoke(error.message ?: "Mat ket noi chat realtime.")
                    }
                    if (shouldReconnect) {
                        scheduleReconnect()
                    }
                }
            }

        hubConnection = created
        return created
    }

    private fun startConnection(connection: HubConnection) {
        cancelReconnect()
        connection.start()
            .doOnComplete {
                reconnectAttempt = 0
                val hadPendingCallbacks = pendingConnectedCallbacks.isNotEmpty()
                notifyConnected()
                if (!hadPendingCallbacks) {
                    rejoinActiveConversation()
                }
            }
            .doOnError { throwable ->
                onError?.invoke(throwable.message ?: "Khong ket noi duoc SignalR.")
                scheduleReconnect()
            }
            .subscribe({}, {})
    }

    private fun notifyConnected() {
        if (pendingConnectedCallbacks.isEmpty()) return
        val callbacks = pendingConnectedCallbacks.toList()
        pendingConnectedCallbacks.clear()
        callbacks.forEach { callback ->
            try {
                callback.invoke()
            } catch (_: Exception) {
                onError?.invoke("Loi xu ly sau khi ket noi SignalR.")
            }
        }
    }

    private fun rejoinActiveConversation() {
        val conversationId = activeConversationId ?: return
        invokeServer("JoinConversation", conversationId)
    }

    private fun invokeServer(methodName: String, vararg args: Any) {
        val connection = hubConnection ?: return
        if (connection.connectionState != HubConnectionState.CONNECTED) return
        connection.invoke(methodName, *args)
            .doOnError { throwable ->
                onError?.invoke(throwable.message ?: "Loi goi hub method $methodName.")
            }
            .subscribe({}, {})
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        val connection = hubConnection ?: return
        if (connection.connectionState == HubConnectionState.CONNECTED ||
            connection.connectionState == HubConnectionState.CONNECTING
        ) {
            return
        }
        if (reconnectRunnable != null) return

        val exponent = min(reconnectAttempt, 5)
        val delay = min(RECONNECT_MAX_DELAY_MS, RECONNECT_BASE_DELAY_MS * (1L shl exponent))
        reconnectAttempt += 1

        reconnectRunnable = Runnable {
            reconnectRunnable = null
            connect()
        }.also { runnable ->
            mainHandler.postDelayed(runnable, delay)
        }
    }

    private fun cancelReconnect() {
        reconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        reconnectRunnable = null
    }

    private fun parseChatMessage(payload: Any?): ChatMessageDTO? {
        val map = payloadAsMap(payload) ?: return null
        val id = asInt(getValueIgnoreCase(map, "id")) ?: return null
        val conversationId = asInt(getValueIgnoreCase(map, "conversationId")) ?: return null
        val senderId = asInt(getValueIgnoreCase(map, "senderId")) ?: return null
        val content = asString(getValueIgnoreCase(map, "content")) ?: return null
        val orderId = asInt(getValueIgnoreCase(map, "orderId"))
        val isRead = asBoolean(getValueIgnoreCase(map, "isRead")) ?: false
        val createdAt = asString(getValueIgnoreCase(map, "createdAt"))

        return ChatMessageDTO(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            orderId = orderId,
            content = content,
            isRead = isRead,
            createdAt = createdAt
        )
    }

    private fun parseTypingEvent(payload: Any?): TypingEvent? {
        val map = payloadAsMap(payload) ?: return null
        val conversationId = asInt(getValueIgnoreCase(map, "conversationId")) ?: return null
        val userId = asInt(getValueIgnoreCase(map, "userId")) ?: return null
        val isTyping = asBoolean(getValueIgnoreCase(map, "isTyping")) ?: return null
        return TypingEvent(conversationId = conversationId, userId = userId, isTyping = isTyping)
    }

    private fun parseMessagesReadEvent(payload: Any?): MessagesReadEvent? {
        val map = payloadAsMap(payload) ?: return null
        val conversationId = asInt(getValueIgnoreCase(map, "conversationId")) ?: return null
        val readerId = asInt(getValueIgnoreCase(map, "readerId")) ?: return null
        val messageIds = asIntList(getValueIgnoreCase(map, "messageIds"))
        return MessagesReadEvent(conversationId = conversationId, readerId = readerId, messageIds = messageIds)
    }

    private fun payloadAsMap(payload: Any?): Map<String, Any?>? {
        if (payload !is Map<*, *>) return null
        return payload.entries
            .filter { it.key != null }
            .associate { it.key.toString() to it.value }
    }

    private fun getValueIgnoreCase(map: Map<String, Any?>, key: String): Any? {
        return map.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
    }

    private fun asInt(value: Any?): Int? {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun asBoolean(value: Any?): Boolean? {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> when {
                value.equals("true", ignoreCase = true) -> true
                value.equals("false", ignoreCase = true) -> false
                value == "1" -> true
                value == "0" -> false
                else -> null
            }
            else -> null
        }
    }

    private fun asString(value: Any?): String? {
        return value?.toString()
    }

    private fun asIntList(value: Any?): List<Int> {
        if (value !is Collection<*>) return emptyList()
        return value.mapNotNull { asInt(it) }
    }
}

data class TypingEvent(
    val conversationId: Int,
    val userId: Int,
    val isTyping: Boolean
)

data class MessagesReadEvent(
    val conversationId: Int,
    val readerId: Int,
    val messageIds: List<Int>
)
