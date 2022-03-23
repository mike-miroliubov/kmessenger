package com.kite.kmessenger.service.messaging

import com.kite.kmessenger.model.ChatMessage
import reactor.core.publisher.Mono

interface BusClient {
    fun sendMessage(to: String, chatMessage: ChatMessage) : Mono<Void>
}