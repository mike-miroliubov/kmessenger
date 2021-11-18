package com.kite.kmessenger.service

import com.kite.kmessenger.model.ChatMessage
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

@Singleton
class ChatService {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChatService::class.java)
    }

    private val localSessions = ConcurrentHashMap<String, Sinks.Many<ChatMessage>>()

    fun sendMessage(message: ChatMessage) {
        val flux = localSessions[message.to] ?: throw RuntimeException()
        flux.tryEmitNext(message)
    }

    fun register(user: String): Flux<ChatMessage> {
        localSessions.putIfAbsent(user, Sinks.many().multicast().directBestEffort())
        return localSessions[user]!!.asFlux()
    }

    fun logout(user: String) {
        val sink = localSessions[user]
        logger.info("Remaining connections count {}", sink?.currentSubscriberCount())
    }
}