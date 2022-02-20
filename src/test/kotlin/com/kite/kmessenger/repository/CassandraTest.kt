package com.kite.kmessenger.repository

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint
import com.kite.kmessenger.model.ChatMessage
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress


class CassandraTest {
    @Test
    fun testCassandra() {
        val session = CqlSession.builder()
            .addContactEndPoint(DefaultEndPoint(InetSocketAddress("localhost", 9042)))
            .withLocalDatacenter("datacenter1")
            .withKeyspace("messenger")
            .build()

        val repository = MessageRepository(session)

        repository.createMessage(ChatMessage("whatever1", "mike", "kite")).block()
    }
}