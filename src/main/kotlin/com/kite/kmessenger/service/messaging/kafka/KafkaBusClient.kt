package com.kite.kmessenger.service.messaging.kafka

import com.kite.kmessenger.model.ChatMessage
import com.kite.kmessenger.service.messaging.BusClient
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.Topic
import reactor.core.publisher.Mono

@KafkaClient
interface KafkaBusClient : BusClient {
    @Topic("default")
    override fun sendMessage(@KafkaKey to: String, chatMessage: ChatMessage) : Mono<Void>
}