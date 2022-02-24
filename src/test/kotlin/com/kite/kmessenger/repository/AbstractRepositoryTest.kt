package com.kite.kmessenger.repository

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName

abstract class AbstractRepositoryTest {
    companion object {
        @Container
        val container = GenericContainer<Nothing>(DockerImageName.parse("cassandra:3.11.9")).apply {
            withEnv(mapOf(
                "MAX_HEAP_SIZE" to "256M",
                "HEAP_NEWSIZE" to "128M"
            ))
            withExposedPorts(9042)
            waitingFor(Wait.defaultWaitStrategy())
            start()
        }
    }
}