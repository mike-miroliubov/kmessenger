package com.kite.kmessenger.repository

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint
import com.kite.kmessenger.model.ChatMessage
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.net.InetSocketAddress
import java.util.*


@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageRepositoryTest : AbstractRepositoryTest() {
    private val session = buildSession()

    private fun buildSession(): CqlSession {
        val session = CqlSession.builder()
            .addContactEndPoint(DefaultEndPoint(InetSocketAddress(container.host, container.firstMappedPort)))
            .withLocalDatacenter("datacenter1")
            .build()

        session.execute("CREATE KEYSPACE messenger WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
        session.close()

        return CqlSession.builder()
            .addContactEndPoint(DefaultEndPoint(InetSocketAddress(container.host, container.firstMappedPort)))
            .withLocalDatacenter("datacenter1")
            .withKeyspace("messenger")
            .build()
    }

    @BeforeAll
    fun init() {
        val schema = MessageRepository::class.java.getResource("/db/migrations/1.cql").readText()
        session.execute(schema)
    }

    @Test
    fun shouldStoreAndRetrieveMessagesByFromAndTo() {
        val repository = MessageRepository(session)

        // given
        val from = "user-${UUID.randomUUID()}"
        val to1 = "user-${UUID.randomUUID()}"
        val to2 = "user-${UUID.randomUUID()}"
        val message1 = ChatMessage("whatever1", to1, from)
        val message2 = ChatMessage("whatever1", to1, from)
        val message3 = ChatMessage("whatever1", to2, from)

        // when
        repository.createMessage(message1)
            .mergeWith(repository.createMessage(message2))
            .mergeWith(repository.createMessage(message3))
            .blockLast()

        val messagesTo1 = repository.getMessagesForChat(from, to1, 10).collectList().block()
        val messagesTo2 = repository.getMessagesForChat(from, to2, 10).collectList().block()

        // then
        assertThat(messagesTo1)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "timestamp")
            .containsExactly(message2, message1)
        assertThat(messagesTo2)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "timestamp")
            .containsExactly(message3)
    }
}