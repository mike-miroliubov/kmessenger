package com.kite.kmessenger.repository

import com.kite.kmessenger.model.ChatMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*


class MessageRepositoryTest : AbstractRepositoryTest() {
    @AfterEach
    fun cleanup() {
        session.execute("TRUNCATE direct_message")
    }

    @Test
    fun `should store and retrieve messages by from and to`() {
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