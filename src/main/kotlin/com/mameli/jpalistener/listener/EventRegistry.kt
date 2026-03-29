package com.mameli.jpalistener.listener

import com.mameli.jpalistener.model.event.EntityCreatedEvent
import com.mameli.jpalistener.model.event.EntityDeletedEvent
import com.mameli.jpalistener.model.event.EntityEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.PayloadApplicationEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

@Component
class EventRegistry(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    
    private var asyncExecutor: Executor? = null
    private val handlerMethods = ConcurrentHashMap<String, MutableList<HandlerMethod>>()

    companion object {
        @JvmStatic var instance: EventRegistry? = null
    }

    init {
        instance = this
    }

    data class HandlerMethod(
        val bean: Any,
        val method: java.lang.reflect.Method,
        val async: Boolean = false
    )

    fun setAsyncExecutor(executor: Executor) {
        this.asyncExecutor = executor
    }

    fun registerHandler(entityClassName: String, bean: Any, method: java.lang.reflect.Method, async: Boolean = false) {
        handlerMethods.computeIfAbsent(entityClassName) { mutableListOf() }
            .add(HandlerMethod(bean, method, async))
    }

    fun publish(event: EntityEvent) {
        handleEvent(event)
    }

    private fun handleEvent(event: EntityEvent) {
        val handlers = mutableListOf<HandlerMethod>()
        handlerMethods[event.entityType.name]?.let { handlers.addAll(it) }
        
        for (handler in handlers) {
            if (handler.async) {
                asyncExecutor?.execute {
                    invokeHandler(handler, event)
                } ?: run {
                    logger.warn("Async handler registered but no executor configured, executing sync")
                    invokeHandler(handler, event)
                }
            } else {
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager
                        .registerSynchronization(object : TransactionSynchronization {
                            override fun afterCommit() {
                                invokeHandler(handler, event)
                            }
                        })
                } else {
                    invokeHandler(handler, event)
                }
            }
        }
        
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager
                .registerSynchronization(object : TransactionSynchronization {
                    override fun afterCommit() {
                        applicationEventPublisher.publishEvent(PayloadApplicationEvent(event, event))
                    }
                })
        } else {
            applicationEventPublisher.publishEvent(PayloadApplicationEvent(event, event))
        }
    }

    private fun invokeHandler(handler: HandlerMethod, event: EntityEvent) {
        try {
            val params = resolveParams(handler.method, event)
            handler.method.invoke(handler.bean, *params.toTypedArray())
        } catch (e: Exception) {
            logger.error("Error invoking handler ${handler.method.name}", e)
        }
    }

    private fun resolveParams(method: java.lang.reflect.Method, event: EntityEvent): List<Any> {
        val params = mutableListOf<Any>()
        val entity = when (event) {
            is EntityCreatedEvent -> event.entity
            is EntityUpdatedEvent -> event.entity
            is EntityDeletedEvent -> event.entity
        }
        for (param in method.parameterTypes) {
            when {
                param.isInstance(event) -> params.add(event)
                param.isInstance(entity) -> params.add(entity)
                event is EntityUpdatedEvent && param.isInstance(event.changeSet) -> params.add(event.changeSet)
                else -> params.add(event)
            }
        }
        return params
    }

    private val logger = LoggerFactory.getLogger(EventRegistry::class.java)
}
