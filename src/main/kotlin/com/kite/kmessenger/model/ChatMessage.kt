package com.kite.kmessenger.model

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class ChatMessage(
    val text: String,
    val to: String,
    val from: String,
    val id: UUID? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
)