package com.kite.kmessenger.repository

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint
import org.cognitor.cassandra.migration.Database
import org.cognitor.cassandra.migration.MigrationConfiguration
import org.cognitor.cassandra.migration.MigrationRepository
import org.cognitor.cassandra.migration.MigrationTask
import org.cognitor.cassandra.migration.keyspace.Keyspace
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
        val session = CqlSession.builder()
            .addContactEndPoint(DefaultEndPoint(InetSocketAddress(cassandra.host, cassandra.firstMappedPort)))
            .withLocalDatacenter("datacenter1")
            .build()

        val db = Database(session, MigrationConfiguration().withKeyspace(Keyspace("messenger")))
        val migration = MigrationTask(db, MigrationRepository("/db/migrations"))
        migration.migrate()
    }
}