package com.kite.kmessenger.service

import com.kite.kmessenger.model.ChatMessage
import com.kite.kmessenger.repository.MessageRepository
import com.kite.kmessenger.service.messaging.BusClient
import com.kite.kmessenger.service.messaging.BusListener
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Singleton
class ChatService(
    @Inject private val messageRepository: MessageRepository,
    @Inject private val busClient: BusClient,
    @Inject private val busListener: BusListener
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChatService::class.java)
    }

    private class UserContext {
        val sessionCount = AtomicInteger()
        val sink: Sinks.Many<ChatMessage> = Sinks.many().multicast().directBestEffort()
    }

    private val localSessions = ConcurrentHashMap<String, UserContext>()

    init {
        busListener.incomingMessages.subscribe(this::forwardMessage)
    }

    fun sendMessage(message: ChatMessage) {
        val flux = localSessions[message.to]

        if (flux == null) {
            busClient.sendMessage(message.to, message) // TODO replace default with topic lookup
        } else {
            val r = flux.sink.tryEmitNext(message)
            logger.info("Forwarded {}: {}", message, r)
        }

        messageRepository.createMessage(message)
    }

    private fun forwardMessage(message: ChatMessage) {
        val flux = localSessions[message.to] ?: return
        val r = flux.sink.tryEmitNext(message)
        logger.info("Forwarded {}: {}", message, r)
    }

    fun register(user: String): Flux<ChatMessage> {
        localSessions.putIfAbsent(user, UserContext())
        val ctx = localSessions[user]!!
        synchronized(ctx) {
            // if other session was the only session, and it just ended, the context got removed from cache
            // we need to put it back
            localSessions.putIfAbsent(user, ctx)
            ctx.sessionCount.incrementAndGet()
            return ctx.sink.asFlux()
        }
    }

    fun logout(user: String) {
        val context = localSessions[user]

        if (context != null) {
            synchronized(context) {
                val subscriberCount = context.sessionCount.decrementAndGet()
                logger.debug("Session of $user closed, $subscriberCount sessions remain")

                if (subscriberCount < 1) {
                    logger.debug("Last session for $user disconnected, no longer online")

                    val result = context.sink.tryEmitComplete()
                    logger.debug("Stream closed for $user: $result")

                    localSessions.remove(user)
                }
            }
        }
    }
}