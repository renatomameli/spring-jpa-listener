package com.mameli.jpalistener.listener

import com.mameli.jpalistener.model.EventMode
import com.mameli.jpalistener.model.event.EntityEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry that manages entity event handler methods and dispatches
 * [EntityEvent]s to them.
 *
 * Handler methods are registered during bean initialization by
 * [com.mameli.jpalistener.config.EntityEventsAutoConfiguration]. Events are published by the entity tracking listeners
 * ([InsertTrackingEventListener], [UpdateTrackingEventListener], [DeleteTrackingEventListener])
 * via the static [instance] singleton — this indirection is
 * necessary because Hibernate's event system has no access to Spring's dependency injection.
 *
 * **Dispatch behavior** depends on the [EventMode] configured per handler:
 * - [EventMode.AFTER_COMMIT]: Handlers are invoked **after commit** (or immediately if no
 *   transaction is active). Exceptions are caught and logged — the transaction is never affected.
 * - [EventMode.TRANSACTIONAL]: Handlers are invoked **synchronously inside** the active
 *   transaction. If a handler throws, the exception propagates and the transaction is rolled back.
 * - Every event is additionally published as a Spring [ApplicationEvent], so standard
 *   `@EventListener` / `@TransactionalEventListener` methods can also receive them.
 *
 * @param applicationEventPublisher the Spring event publisher for broadcasting events
 */
class EventRegistry(private val applicationEventPublisher: ApplicationEventPublisher) {
    companion object {
        /** Global singleton instance, set during initialization and used by the entity tracking listeners. */
        @JvmStatic
        var instance: EventRegistry? = null
            private set

        private val log = LoggerFactory.getLogger(EventRegistry::class.java)
    }

    private val handlerMethods = ConcurrentHashMap<String, MutableList<HandlerMethod>>()


    init {
        instance = this
    }

    private data class HandlerMethod(
        val bean: Any,
        val method: java.lang.reflect.Method,
        val eventType: Class<out EntityEvent>,
        val mode: EventMode
    )

    /**
     * Registers a handler method for a specific entity type, event type and dispatch mode.
     *
     * @param entityClassName fully qualified class name of the entity to listen for
     * @param bean the Spring bean instance containing the handler method
     * @param method the handler method to invoke when a matching event occurs
     * @param eventType the specific [EntityEvent] subclass this handler expects
     * @param mode the [EventMode] controlling when the handler runs relative to the transaction
     */
    fun registerHandler(
        entityClassName: String,
        bean: Any,
        method: java.lang.reflect.Method,
        eventType: Class<out EntityEvent>,
        mode: EventMode
    ) {
        handlerMethods.computeIfAbsent(entityClassName) { mutableListOf() }
            .add(HandlerMethod(bean, method, eventType, mode))
    }

    /**
     * Dispatches an [EntityEvent] to all registered handlers for the event's entity type
     * and publishes it as a Spring [ApplicationEvent].
     *
     * - **[EventMode.TRANSACTIONAL]** handlers are invoked immediately (inside the transaction).
     *   Exceptions propagate and will cause the transaction to roll back.
     * - **[EventMode.AFTER_COMMIT]** handlers are deferred until after the transaction commits
     *   (or invoked immediately if no transaction is active). Exceptions are caught and logged.
     *
     * @param event the entity event to dispatch
     */
    fun publish(event: EntityEvent) {
        val handlers = handlerMethods[event.entityType.name] ?: emptyList()

        for (handler in handlers) {
            if (!handler.eventType.isInstance(event)) continue

            when (handler.mode) {
                EventMode.TRANSACTIONAL -> invokeHandlerTransactional(handler, event)
                EventMode.AFTER_COMMIT, EventMode.UNSET -> afterCommitOrNow { invokeHandlerSafe(handler, event) }
            }
        }

        applicationEventPublisher.publishEvent(event)
    }

    /**
     * Executes [action] after the current transaction commits, or immediately if no
     * transaction is active.
     */
    private fun afterCommitOrNow(action: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    action()
                }
            })
        } else {
            action()
        }
    }

    /**
     * Invokes the handler inside the current transaction. Exceptions are **not** caught —
     * they propagate to the caller and will cause the transaction to roll back.
     */
    private fun invokeHandlerTransactional(handler: HandlerMethod, event: EntityEvent) {
        val params = resolveParams(handler.method, event)
        try {
            handler.method.invoke(handler.bean, *params.toTypedArray())
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // Unwrap the real exception so the caller sees the original cause
            throw e.targetException
        }
    }

    /**
     * Invokes the handler safely — exceptions are caught and logged.
     * Used for [EventMode.AFTER_COMMIT] handlers where the transaction has already committed.
     */
    private fun invokeHandlerSafe(handler: HandlerMethod, event: EntityEvent) {
        try {
            val params = resolveParams(handler.method, event)
            handler.method.invoke(handler.bean, *params.toTypedArray())
        } catch (e: Exception) {
            log.error("Error invoking handler ${handler.method.name}", e)
        }
    }

    /**
     * Resolves method parameters by matching them against the event, the entity,
     * or the change set (for update events).
     */
    private fun resolveParams(method: java.lang.reflect.Method, event: EntityEvent): List<Any> {
        return method.parameterTypes.map { param ->
            when {
                param.isInstance(event) -> event
                param.isInstance(event.entity) -> event.entity
                event is EntityUpdatedEvent && param.isInstance(event.changeSet) -> event.changeSet
                else -> throw IllegalArgumentException(
                    "Cannot resolve parameter of type ${param.name} for handler ${method.declaringClass.simpleName}.${method.name} " +
                            "from event ${event::class.simpleName}"
                )
            }
        }
    }
}
