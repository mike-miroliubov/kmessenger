package com.kite.kmessenger.repository

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.kite.kmessenger.model.ChatMessage
import jakarta.inject.Inject
import jakarta.inject.Singleton
import reactor.core.publisher.Mono

@Singleton
class MessageRepository(@Inject val cqlSession: CqlSession) {
    fun createMessage(chatMessage: ChatMessage): Mono<AsyncResultSet> {
        val statement = QueryBuilder.insertInto("direct_message")
            .value("from_user", QueryBuilder.bindMarker())
            .value("to_user", QueryBuilder.bindMarker())
            .value("content", QueryBuilder.bindMarker())
            .value("sent_at", QueryBuilder.toTimestamp(QueryBuilder.now()))
            .build()

        return Mono.fromCompletionStage(cqlSession.prepareAsync(statement))
            .map {
                it.bind()
                    .setString("from_user", chatMessage.from)
                    .setString("to_user", chatMessage.to)
                    .setString("content", chatMessage.text)
            }
            .flatMap { Mono.fromCompletionStage(cqlSession.executeAsync(it)) }

        /*val bound = cqlSession.prepare(statement).bind()
            .setString("from_user", chatMessage.from)
            .setString("to_user", chatMessage.to)
            .setString("content", chatMessage.text)

        cqlSession.execute(bound)*/
    }
}