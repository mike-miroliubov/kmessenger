package com.kite.kmessenger

import com.kite.kmessenger.model.ChatMessage
import com.kite.kmessenger.service.ChatService
import com.kite.kmessenger.util.getLogger
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.websocket.WebSocketClient
import io.micronaut.websocket.annotation.ClientWebSocket
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import jakarta.inject.Inject
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.test.appender.ListAppender
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedDeque

@MicronautTest
class ChatServerWebSocketIT {
    @Inject lateinit var context: ApplicationContext
    @Inject lateinit var server: EmbeddedServer

    lateinit var user1: TestWebSocketClient
    lateinit var user2: TestWebSocketClient
    lateinit var user3: TestWebSocketClient

    @ClientWebSocket
    abstract class TestWebSocketClient : AutoCloseable {
        val messages = ConcurrentLinkedDeque<ChatMessage>()

        @OnOpen
        fun onOpen() {}

        @OnMessage
        fun onMessage(message: ChatMessage) = messages.push(message)

        @OnClose
        fun onClose() {}

        abstract fun send(message: ChatMessage)
    }

    private fun createClient(name: String): TestWebSocketClient {
        val client = context.getBean(WebSocketClient::class.java)
        return Flux.from(client.connect(TestWebSocketClient::class.java, "ws://localhost:${server.port}/chat/${name}")).blockFirst()
    }

    @BeforeEach
    fun init() {
        user1 = createClient("user1")
        user2 = createClient("user2")
        user3 = createClient("user3")
    }

    @Test
    fun shouldSendPrivateMessage() {
        //given
        val message = ChatMessage("Hello", "user2", "user1")

        // when
        user1.send(message)

        // then
        await.until { user2.messages.contains(message) }

        waitAndCheck(Duration.ofSeconds(1)) { user3.messages.isEmpty() }
    }

    private fun waitAndCheck(duration: Duration, check: () -> Boolean) {
        Thread.sleep(duration.toMillis())
        Assertions.assertThat(check()).isTrue()
    }

    private fun waitAndCheck(duration: Duration, checks: List<() -> Boolean>) {
        Thread.sleep(duration.toMillis())
        checks.forEach { Assertions.assertThat(it()).isTrue() }
    }

    @Test
    fun shouldDeliverToMultipleClients() {
        // given
        val user2client2 = createClient("user2")
        val user2client3 = createClient("user2")

        val message = ChatMessage("Hello", "user2", "user1")

        // when
        user1.send(message)

        // then
        await.until { user2.messages.contains(message) }
        await.until { user2client2.messages.contains(message) }
        await.until { user2client3.messages.contains(message) }

        waitAndCheck(Duration.ofSeconds(1)) { user3.messages.isEmpty() }

        // when - check that 1 client session disconnect does not prevent others from receiving messages
        user2client2.close()

        Thread.sleep(100)
        val message2 = ChatMessage("Did we lose someone?", "user2", "user1")
        user1.send(message2)

        // then
        await.until { user2.messages.contains(message2) }
        await.until { user2client3.messages.contains(message2) }
        waitAndCheck(Duration.ofSeconds(1), listOf(
            { user3.messages.isEmpty() },
            { !user2client2.messages.contains(message2) }
        ))

        // when - check that when new client comes online it gets new messages
        val user2client4 = createClient("user2")
        Thread.sleep(100)
        val message3 = ChatMessage("Now there's more of us", "user2", "user1")
        user1.send(message3)

        // then
        await.until { user2.messages.contains(message3) }
        await.until { user2client3.messages.contains(message3) }
        await.until { user2client4.messages.contains(message3) }
        waitAndCheck(Duration.ofSeconds(1), listOf(
            { user3.messages.isEmpty() },
            { !user2client2.messages.contains(message3) }
        ))
    }

    @Test
    internal fun shouldCorrectlyCloseMultipleSessions() {
        // given
        val logger = getLogger(ChatService::class.java.name, Logger::class.java)!!
        val listAppender = ListAppender("test")
        listAppender.start()

        logger.addAppender(listAppender)

        val client2 = createClient("user2")
        val client3 = createClient("user2")

        // when
        client2.close()

        // then
        Assertions.assertThat(listAppender.messages.any { it.endsWith("Session of user2 closed, 2 sessions remain") })

        // when
        val client4 = createClient("user2")
        val client5 = createClient("user2")

        client5.close()
        client4.close()
        client3.close()
        user2.close()

        // then
        Assertions.assertThat(listAppender.messages.any { it.endsWith("Session of user2 closed, 3 sessions remain") })
        Assertions.assertThat(listAppender.messages.any { it.endsWith("Session of user2 closed, 2 sessions remain") })
        Assertions.assertThat(listAppender.messages.any { it.endsWith("Session of user2 closed, 1 sessions remain") })
        Assertions.assertThat(listAppender.messages.any { it.endsWith("Session of user2 closed, 0 sessions remain") })
        Assertions.assertThat(listAppender.messages.any { it.endsWith("Last session for user2 disconnected, no longer online") })
        Assertions.assertThat(listAppender.messages.any { it.endsWith("Stream closed for user2: OK") })
    }
}