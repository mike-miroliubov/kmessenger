package com.kite.kmessenger.service.messaging

import com.kite.kmessenger.model.ChatMessage
import java.util.concurrent.Future

interface BusClient {
    fun sendMessage(to: String, chatMessage: ChatMessage) : Future<Void>
}