package com.kite.kmessenger.service.messaging.kafka

import com.kite.kmessenger.model.ChatMessage
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import kotlin.concurrent.thread


@MicronautTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KafkaBusListenerTest : TestPropertyProvider {
    companion object {
        @Container
        val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
        val logger = LoggerFactory.getLogger(KafkaBusListenerTest.javaClass)
    }

    @Inject
    lateinit var kafkaBusListener: KafkaBusListener

    @Inject
    lateinit var kafkaBusClient: KafkaBusClient


    @Test
    fun `should send and receive 10 messages`() {
        // given
        val msg = ChatMessage("hello", "user1", "user2")
        val incoming = mutableListOf<ChatMessage>()

        // when
        thread {
            (0 until 10).map { kafkaBusClient.sendMessage(msg.to, msg) }
        }

        kafkaBusListener.incomingMessages.subscribe { incoming += it }

        // then
        await atMost Duration.ofSeconds(10) until {
            incoming.size == 10
        }

        assertThat(incoming).containsOnly(msg)
        assertThat(incoming).size().isEqualTo(10)
    }

    override fun getProperties(): Map<String, String> {
        kafkaContainer.start()
        return mapOf(
            "kafka.bootstrap.servers" to kafkaContainer.bootstrapServers
        )
    }
}
