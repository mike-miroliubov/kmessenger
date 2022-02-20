package com.kite.kmessenger.model

import java.time.LocalDateTime
import java.time.ZoneOffset

data class ChatMessage(
    val text: String,
    val to: String,
    val from: String
) {
    val timestamp: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
}