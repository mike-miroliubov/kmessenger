package com.kite.kmessenger.controller

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.websocket.WebSocketBroadcaster
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@ServerWebSocket("/chat/{username}")
class ChatServerWebSocket(private val broadcaster: WebSocketBroadcaster) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChatServerWebSocket::class.java)
        private val privateMessageRegex = Regex("^@(\\S+) (.*)$")
    }

    private val localSessions = ConcurrentHashMap<String, Set<WebSocketSession>>()

    @OnOpen
    fun onOpen(username: String, session: WebSocketSession): Publisher<String> {
        logger.info("[$username] joined!")

        localSessions.compute(username) { _, value ->
            if (value == null) {
                broadcaster.broadcastAsync("[$username] joined!")
                setOf(session)
            } else {
                value + setOf(session)
            }
        }

        return session.send("Welcome, $username")
    }

    @OnMessage
    fun onMessage(username: String, message: String, session: WebSocketSession): Publisher<*> {
        val msg = "[$username] $message"
        logger.debug(msg)
        val match = privateMessageRegex.matchEntire(message)

        if (match != null) {
            val (address, privateMessage) = match.destructured
            val addressSessions = localSessions[address]
            return if (addressSessions == null) {
                session.send("User $address not found")
            } else {
                Publishers.fromCompletableFuture(
                    CompletableFuture.allOf(*addressSessions.map { it.sendAsync(msg) }.toTypedArray())
                )
            }
        }

        return broadcaster.broadcast(msg)
    }

    @OnClose
    fun onClose(username: String, session: WebSocketSession): Publisher<String> {
        val msg = "[$username] Disconnected!"
        logger.info(msg)
        localSessions.merge(username, setOf(session)) { v1, v2 -> (v1 - v2).ifEmpty { null } } ?:
            return broadcaster.broadcast(msg)

        return Publishers.empty()
    }
}