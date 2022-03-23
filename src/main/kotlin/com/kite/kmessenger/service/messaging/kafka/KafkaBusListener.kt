package com.kite.kmessenger.service.messaging.kafka

import com.kite.kmessenger.model.ChatMessage
import com.kite.kmessenger.service.messaging.BusListener
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.Topic
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@KafkaListener(uniqueGroupId = true)
class KafkaBusListener : BusListener {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(KafkaBusListener::class.java)
    }

    private val messages: Sinks.Many<ChatMessage> = Sinks.many().unicast().onBackpressureBuffer()

    override val incomingMessages: Flux<ChatMessage>
        get() = this.messages.asFlux()

    @Topic("\${messenger.topic}")
    fun receiveMessage(chatMessage: ChatMessage) {
        LOGGER.debug("Received a message through Kafka: $chatMessage")
        val result = messages.tryEmitNext(chatMessage)
        if (result.isFailure) {
            LOGGER.error("Failed to receive the message: $result")
        }
    }
}