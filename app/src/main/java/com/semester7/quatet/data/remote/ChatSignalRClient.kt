package com.semester7.quatet.data.remote

import android.content.Context
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.semester7.quatet.data.local.SessionManager
import io.reactivex.rxjava3.core.Single

class ChatSignalRClient(context: Context) {

    companion object {
        private const val HUB_URL = "http://14.225.207.221:5000/hubs/chat"
    }

    private val appContext = context.applicationContext
    private var hubConnection: HubConnection? = null

    private var onReceiveMessage: (() -> Unit)? = null
    private var onTypingChanged: ((Boolean) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun setCallbacks(
        onReceiveMessage: (() -> Unit)? = null,
        onTypingChanged: ((Boolean) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        this.onReceiveMessage = onReceiveMessage
        this.onTypingChanged = onTypingChanged
        this.onError = onError
    }

    fun connect(onConnected: (() -> Unit)? = null) {
        val token = SessionManager.getToken(appContext).orEmpty()
        if (token.isBlank()) {
            onError?.invoke("Chua dang nhap, khong the ket noi chat realtime.")
            return
        }

        if (hubConnection == null) {
            hubConnection = HubConnectionBuilder.create(HUB_URL)
                .withAccessTokenProvider(Single.defer { Single.just(SessionManager.getToken(appContext).orEmpty()) })
                .build()
                .apply {
                    on("ReceiveMessage", {
                        onReceiveMessage?.invoke()
                    }, Any::class.java)

                    on("UserTyping", { payload ->
                        val value = extractBoolean(payload, "IsTyping")
                        onTypingChanged?.invoke(value)
                    }, Any::class.java)

                    onClosed { error ->
                        if (error != null) {
                            onError?.invoke(error.message ?: "Mat ket noi chat realtime.")
                        }
                    }
                }
        }

        val connection = hubConnection ?: return
        when (connection.connectionState) {
            HubConnectionState.CONNECTED -> onConnected?.invoke()
            HubConnectionState.CONNECTING -> Unit
            else -> {
                connection.start()
                    .doOnComplete { onConnected?.invoke() }
                    .doOnError { throwable ->
                        onError?.invoke(throwable.message ?: "Khong ket noi duoc SignalR.")
                    }
                    .subscribe({}, {})
            }
        }
    }

    fun joinConversation(conversationId: Int) {
        val connection = hubConnection ?: return
        if (connection.connectionState == HubConnectionState.CONNECTED) {
            connection.send("JoinConversation", conversationId)
        }
    }

    fun leaveConversation(conversationId: Int) {
        val connection = hubConnection ?: return
        if (connection.connectionState == HubConnectionState.CONNECTED) {
            connection.send("LeaveConversation", conversationId)
        }
    }

    fun sendTyping(conversationId: Int, isTyping: Boolean) {
        val connection = hubConnection ?: return
        if (connection.connectionState == HubConnectionState.CONNECTED) {
            connection.send("SendTyping", conversationId, isTyping)
        }
    }

    fun disconnect() {
        val connection = hubConnection ?: return
        if (connection.connectionState != HubConnectionState.DISCONNECTED) {
            connection.stop().subscribe({}, {})
        }
        hubConnection = null
    }

    private fun extractBoolean(payload: Any?, key: String): Boolean {
        if (payload == null) return false
        val raw = payload.toString()
        val pattern = "\"$key\":true"
        return raw.contains(pattern, ignoreCase = true)
    }
}
