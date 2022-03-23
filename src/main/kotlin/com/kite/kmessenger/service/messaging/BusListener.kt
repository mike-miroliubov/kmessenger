package com.kite.kmessenger.service.messaging

import com.kite.kmessenger.model.ChatMessage
import reactor.core.publisher.Flux

interface BusListener {
    val incomingMessages: Flux<ChatMessage>
}