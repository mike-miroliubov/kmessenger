package com.kite.kmessenger.service.messaging.kafka

import com.kite.kmessenger.model.ChatMessage
import com.kite.kmessenger.service.messaging.BusClient
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.Topic
import java.util.concurrent.Future

@KafkaClient(acks = KafkaClient.Acknowledge.NONE)
interface KafkaBusClient : BusClient {
    @Topic("default") // returning Future, not Mono, because we are in fire and forget mode
    override fun sendMessage(@KafkaKey to: String, chatMessage: ChatMessage) : Future<Void>
}