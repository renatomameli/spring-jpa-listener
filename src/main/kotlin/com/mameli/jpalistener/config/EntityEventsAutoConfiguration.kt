package com.mameli.jpalistener.config

import com.mameli.jpalistener.annotation.OnCreate
import com.mameli.jpalistener.annotation.OnDelete
import com.mameli.jpalistener.annotation.OnUpdate
import com.mameli.jpalistener.listener.EventRegistry
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import com.mameli.jpalistener.model.event.EntityDeletedEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.annotation.AnnotationUtils

/**
 * Spring Boot auto-configuration for entity event tracking and publishing.
 *
 * Implements [BeanPostProcessor] directly to discover handler methods annotated with
 * [@OnCreate], [@OnUpdate], or [@OnDelete] on every bean during initialization and
 * registers them with the [EventRegistry].
 *
 * **No explicit configuration is needed** - this auto-configuration is automatically enabled
 * by Spring Boot's auto-configuration mechanism. Users only need to:
 * 1. Mark entities with `@TrackedEntity`
 * 2. Create handler methods with `@OnCreate`, `@OnUpdate`, `@OnDelete` annotations
 *
 * @see com.mameli.jpalistener.annotation.TrackedEntity
 * @see com.mameli.jpalistener.annotation.OnCreate
 * @see com.mameli.jpalistener.annotation.OnUpdate
 * @see com.mameli.jpalistener.annotation.OnDelete
 */
@AutoConfiguration
open class EntityEventsAutoConfiguration(applicationEventPublisher: ApplicationEventPublisher) : BeanPostProcessor {
    companion object {
        private val log = LoggerFactory.getLogger(EntityEventsAutoConfiguration::class.java)
    }

    private val eventRegistry = EventRegistry(applicationEventPublisher)

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        for (method in bean.javaClass.declaredMethods) {
            AnnotationUtils.findAnnotation(method, OnCreate::class.java)?.let {
                eventRegistry.registerHandler(it.entityClass.java.name, bean, method, EntityCreatedEvent::class.java)
                log.info("Registered @OnCreate: ${bean.javaClass.simpleName}.${method.name} -> ${it.entityClass.simpleName}")
            }

            AnnotationUtils.findAnnotation(method, OnUpdate::class.java)?.let {
                eventRegistry.registerHandler(it.entityClass.java.name, bean, method, EntityUpdatedEvent::class.java)
                log.info("Registered @OnUpdate: ${bean.javaClass.simpleName}.${method.name} -> ${it.entityClass.simpleName}")
            }

            AnnotationUtils.findAnnotation(method, OnDelete::class.java)?.let {
                eventRegistry.registerHandler(it.entityClass.java.name, bean, method, EntityDeletedEvent::class.java)
                log.info("Registered @OnDelete: ${bean.javaClass.simpleName}.${method.name} -> ${it.entityClass.simpleName}")
            }
        }
        return bean
    }
}
