package com.kite.kmessenger.service

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class KafkaTest {
    @Test
    fun testSendReceive() {
        val topic = "test-topic"

        KafkaProducer<String, String>(Properties().apply {
            this["bootstrap.servers"] = "localhost:9092"
            this["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
            this["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        }).use { producer ->
            for (i in 0 until 10) {
                producer.send(ProducerRecord(topic, "hello")).get()
            }
        }

        Thread.sleep(1000)

        KafkaConsumer<String, String>(Properties().apply {
            this["bootstrap.servers"] = "localhost:9092"
            this["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer";
            this["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer";
            this["group.id"] = "test-group";
        }).use { consumer ->
            consumer.subscribe(listOf(topic))
            val records = consumer.poll(Duration.ofSeconds(5))
            records.forEach { println(it) }
        }
    }
}