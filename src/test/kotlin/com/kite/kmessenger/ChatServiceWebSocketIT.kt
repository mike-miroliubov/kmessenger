package com.kite.kmessenger

import com.kite.kmessenger.model.ChatMessage
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.websocket.WebSocketClient
import io.micronaut.websocket.annotation.ClientWebSocket
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import jakarta.inject.Inject
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit

@MicronautTest
class ChatServiceWebSocketIT {
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
        Assertions.assertTrue(check())
    }

    private fun waitAndCheck(duration: Duration, checks: List<() -> Boolean>) {
        Thread.sleep(duration.toMillis())
        checks.forEach { Assertions.assertTrue(it()) }
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
}