package com.mameli.jpalistener.listener

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
 * **Dispatch behavior:**
 * - If a transaction is active, handlers are invoked **after commit** to ensure data consistency.
 * - If no transaction is active, handlers are invoked immediately.
 * - Every event is additionally published as a Spring [ApplicationEvent], so standard
 *   `@EventListener` methods can also receive them.
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
        val eventType: Class<out EntityEvent>
    )

    /**
     * Registers a handler method for a specific entity type and event type.
     *
     * @param entityClassName fully qualified class name of the entity to listen for
     * @param bean the Spring bean instance containing the handler method
     * @param method the handler method to invoke when a matching event occurs
     * @param eventType the specific [EntityEvent] subclass this handler expects
     */
    fun registerHandler(entityClassName: String, bean: Any, method: java.lang.reflect.Method, eventType: Class<out EntityEvent>) {
        handlerMethods.computeIfAbsent(entityClassName) { mutableListOf() }.add(HandlerMethod(bean, method, eventType))
    }

    /**
     * Dispatches an [EntityEvent] to all registered handlers for the event's entity type
     * and publishes it as a Spring [ApplicationEvent].
     *
     * If a transaction is active, both handler invocations and the Spring event are deferred
     * until after the transaction commits.
     *
     * @param event the entity event to dispatch
     */
    fun publish(event: EntityEvent) {
        val handlers = handlerMethods[event.entityType.name] ?: emptyList()

        for (handler in handlers) {
            if (!handler.eventType.isInstance(event)) continue
            afterCommitOrNow { invokeHandler(handler, event) }
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

    private fun invokeHandler(handler: HandlerMethod, event: EntityEvent) {
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
