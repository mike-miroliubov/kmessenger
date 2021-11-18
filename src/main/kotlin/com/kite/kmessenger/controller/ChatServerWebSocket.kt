package com.kite.kmessenger.controller

import com.kite.kmessenger.model.ChatMessage
import com.kite.kmessenger.service.ChatService
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.websocket.WebSocketBroadcaster
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import jakarta.inject.Inject
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@ServerWebSocket("/chat/{username}")
class ChatServerWebSocket(
        private val broadcaster: WebSocketBroadcaster,
        @Inject private val chatService: ChatService
    ) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChatServerWebSocket::class.java)
        private val privateMessageRegex = Regex("^@(\\S+) (.*)$")
    }

    private val localSessions = ConcurrentHashMap<String, Set<WebSocketSession>>()

    @OnOpen
    fun onOpen(username: String, session: WebSocketSession): Publisher<ChatMessage> {
        logger.info("[$username] joined!")
        val flux = chatService.register(username)
        return flux.flatMap { session.send(it) }
    }

    @OnMessage
    fun onMessage(username: String, message: ChatMessage, session: WebSocketSession) {
        val msg = "[$username] $message"
        logger.debug(msg)
        chatService.sendMessage(message)
    }

    @OnClose
    fun onClose(username: String, session: WebSocketSession): Publisher<String> {
        val msg = "[$username] Disconnected!"
        logger.info(msg)

        return Publishers.empty()
    }
}