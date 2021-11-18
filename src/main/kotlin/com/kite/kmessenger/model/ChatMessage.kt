package com.kite.kmessenger.model

data class ChatMessage(
    val text: String,
    val to: String,
    val from: String
)