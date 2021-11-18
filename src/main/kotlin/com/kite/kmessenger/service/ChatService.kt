package com.kite.kmessenger.service

import com.kite.kmessenger.model.ChatMessage
import jakarta.inject.Singleton
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

@Singleton
class ChatService {
    val localSessions = ConcurrentHashMap<String, Sinks.Many<ChatMessage>>()

    fun sendMessage(message: ChatMessage, to: String): Flux<ChatMessage> {
        val flux = localSessions[to] ?: throw RuntimeException()
        flux.tryEmitNext(message)
        return flux.asFlux()
    }

    fun register(user: String): Flux<ChatMessage> {
        localSessions.putIfAbsent(user, Sinks.many().multicast().directBestEffort())
        return localSessions[user]!!.asFlux()
    }
}