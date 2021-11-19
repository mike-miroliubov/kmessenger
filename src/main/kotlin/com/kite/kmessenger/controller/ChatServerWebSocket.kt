package com.kite.kmessenger.controller

import com.kite.kmessenger.exception.UserNotFoundException
import com.kite.kmessenger.model.ChatMessage
import com.kite.kmessenger.service.ChatService
import io.micronaut.websocket.WebSocketBroadcaster
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.*
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

@ServerWebSocket("/chat/{username}")
class ChatServerWebSocket(
        private val broadcaster: WebSocketBroadcaster,
        @Inject private val chatService: ChatService
    ) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChatServerWebSocket::class.java)
    }

    @OnOpen
    fun onOpen(username: String, session: WebSocketSession): Flux<ChatMessage> {
        logger.info("[$username] joined!")
        val flux = chatService.register(username)
        return flux.flatMap { session.send(it) }
    }

    @OnMessage
    fun onMessage(username: String, message: ChatMessage, session: WebSocketSession) {
        try {
            chatService.sendMessage(message)
        } catch (e: UserNotFoundException) {
            logger.error("Message {} cannot be delivered", message, e)
            session.sendAsync("Message cannot be delivered: ${e.message}")
        }
    }

    @OnClose
    fun onClose(username: String, session: WebSocketSession) {
        val msg = "[$username] Disconnected!"
        logger.info(msg)
        chatService.logout(username)
    }

    @OnError
    fun handleUnexpectedError(username: String, session: WebSocketSession, throwable: Throwable) {
        chatService.logout(username)
        session.sendAsync("Fatal error occurred: ${throwable.message}")
        throw throwable
    }
}