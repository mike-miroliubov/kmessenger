package com.kite.kmessenger.repository

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint
import com.kite.kmessenger.model.ChatMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.*


class MessageRepositoryTest {
    @Test
    fun shouldStoreAndRetrieveMessagesByFromAndTo() {
        val session = CqlSession.builder()
            .addContactEndPoint(DefaultEndPoint(InetSocketAddress("localhost", 9042)))
            .withLocalDatacenter("datacenter1")
            .withKeyspace("messenger")
            .build()

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
            .containsExactlyInAnyOrder(message1, message2)
        assertThat(messagesTo2)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "timestamp")
            .containsExactly(message3)
    }
}