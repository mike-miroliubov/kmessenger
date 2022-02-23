package com.kite.kmessenger.repository

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.MappedAsyncPagingIterable
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.type.reflect.GenericType
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import com.datastax.oss.driver.api.querybuilder.relation.Relation
import com.kite.kmessenger.model.ChatMessage
import jakarta.inject.Inject
import jakarta.inject.Singleton
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

private const val DIRECT_MESSAGE_TABLE = "direct_message"
private const val FROM_USER_FIELD = "from_user"
private const val TO_USER_FIELD = "to_user"
private const val CONTENT_FIELD = "content"
private const val ID_FIELD = "id"

private const val SENT_AT_FIELD = "sent_at"

@Singleton
class MessageRepository(@Inject val cqlSession: CqlSession) {
    fun createMessage(chatMessage: ChatMessage): Mono<AsyncResultSet> {
        val statement = insertInto(DIRECT_MESSAGE_TABLE)
            .value(FROM_USER_FIELD, QueryBuilder.bindMarker())
            .value(TO_USER_FIELD, QueryBuilder.bindMarker())
            .value(CONTENT_FIELD, QueryBuilder.bindMarker())
            .value(ID_FIELD, QueryBuilder.now())
            .build()

        return Mono.fromCompletionStage(
            cqlSession.prepareAsync(statement)
                .thenApply {
                    it.bind()
                        .setString(FROM_USER_FIELD, chatMessage.from)
                        .setString(TO_USER_FIELD, chatMessage.to)
                        .setString(CONTENT_FIELD, chatMessage.text)
                }
                .thenCompose { cqlSession.executeAsync(it) }
        )
    }

    // TODO: use paging state instead of baseline UUID?
    fun getMessagesForChat(from: String, to: String, pageSize: Int, baseline: UUID? = null): Flux<ChatMessage> {
        val where = mutableListOf(
            Relation
                .column(TO_USER_FIELD)
                .isEqualTo(QueryBuilder.bindMarker()),
            Relation
                .column(FROM_USER_FIELD)
                .isEqualTo(QueryBuilder.bindMarker()))

        if (baseline != null) {
            where += Relation.column(ID_FIELD).isGreaterThan(QueryBuilder.bindMarker())
        }

        val statement = selectFrom(DIRECT_MESSAGE_TABLE)
            .columns(TO_USER_FIELD, FROM_USER_FIELD, ID_FIELD, CONTENT_FIELD)
            //.column(CqlIdentifier.fromCql("toTimestamp(${ID_FIELD}) as ${SENT_AT_FIELD}"))
            .where(where)
            .limit(pageSize)
            .build()

        return Mono.fromCompletionStage(cqlSession.prepareAsync(statement))
            .map {
                val bind = it.bind()
                    .setString(TO_USER_FIELD, to)
                    .setString(FROM_USER_FIELD, from)

                if (baseline != null) {
                    return@map bind.setUuid(ID_FIELD, baseline)
                }

                bind
            }
            .flatMapMany { Flux.from(cqlSession.executeReactive(it)) }
            .map(this::messageFromRow)
    }

    private fun messageFromRow(row: Row): ChatMessage = ChatMessage(
        id = row[ID_FIELD, UUID::class.java],
        to = row[TO_USER_FIELD, String::class.java]!!,
        from = row[FROM_USER_FIELD, String::class.java]!!,
        text = row[CONTENT_FIELD, String::class.java] ?: ""
        //row[SENT_AT_FIELD, GenericType.LOCAL_DATE_TIME]!!
    )
}