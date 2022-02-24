package com.kite.kmessenger.repository

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.CassandraContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.InetSocketAddress


@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractRepositoryTest {
    companion object {
        @Container
        val cassandra = CassandraContainer<Nothing>("cassandra:3.11.9").apply {
            withInitScript("db/init/1.cql")
            start()
        }
    }

    protected val session = CqlSession.builder()
        .addContactEndPoint(DefaultEndPoint(InetSocketAddress(cassandra.host, cassandra.firstMappedPort)))
        .withLocalDatacenter("datacenter1")
        .withKeyspace("messenger")
        .build()

    @BeforeAll
    fun init() {
        val schema = MessageRepository::class.java.getResource("/db/migrations/1.cql").readText()
        session.execute(schema)
    }
}