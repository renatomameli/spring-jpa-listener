package com.mameli.jpalistener.config

import com.mameli.jpalistener.annotation.OnCreate
import com.mameli.jpalistener.annotation.OnDelete
import com.mameli.jpalistener.annotation.OnUpdate
import com.mameli.jpalistener.listener.EventRegistry
import com.mameli.jpalistener.model.EventMode
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import com.mameli.jpalistener.model.event.EntityDeletedEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
 * The dispatch mode can be configured:
 * - **Per handler**: via the `mode` parameter on the annotation (e.g. `@OnCreate(Product::class, mode = EventMode.TRANSACTIONAL)`)
 * - **Globally**: via `spring.jpalistener.default-mode` in `application.yml` (defaults to `AFTER_COMMIT`)
 *
 * @see com.mameli.jpalistener.annotation.TrackedEntity
 * @see com.mameli.jpalistener.annotation.OnCreate
 * @see com.mameli.jpalistener.annotation.OnUpdate
 * @see com.mameli.jpalistener.annotation.OnDelete
 * @see JpaListenerProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(JpaListenerProperties::class)
open class EntityEventsAutoConfiguration(
    applicationEventPublisher: ApplicationEventPublisher,
    private val properties: JpaListenerProperties
) : BeanPostProcessor {
    companion object {
        private val log = LoggerFactory.getLogger(EntityEventsAutoConfiguration::class.java)
    }

    private val eventRegistry = EventRegistry(applicationEventPublisher)

    /**
     * Resolves [EventMode.UNSET] to the globally configured default mode.
     */
    private fun resolveMode(annotationMode: EventMode): EventMode =
        if (annotationMode == EventMode.UNSET) properties.defaultMode else annotationMode

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        for (method in bean.javaClass.declaredMethods) {
            AnnotationUtils.findAnnotation(method, OnCreate::class.java)?.let {
                val mode = resolveMode(it.mode)
                eventRegistry.registerHandler(it.entityClass.java.name, bean, method, EntityCreatedEvent::class.java, mode)
                log.info("Registered @OnCreate(${mode}): ${bean.javaClass.simpleName}.${method.name} -> ${it.entityClass.simpleName}")
            }

            AnnotationUtils.findAnnotation(method, OnUpdate::class.java)?.let {
                val mode = resolveMode(it.mode)
                eventRegistry.registerHandler(it.entityClass.java.name, bean, method, EntityUpdatedEvent::class.java, mode)
                log.info("Registered @OnUpdate(${mode}): ${bean.javaClass.simpleName}.${method.name} -> ${it.entityClass.simpleName}")
            }

            AnnotationUtils.findAnnotation(method, OnDelete::class.java)?.let {
                val mode = resolveMode(it.mode)
                eventRegistry.registerHandler(it.entityClass.java.name, bean, method, EntityDeletedEvent::class.java, mode)
                log.info("Registered @OnDelete(${mode}): ${bean.javaClass.simpleName}.${method.name} -> ${it.entityClass.simpleName}")
            }
        }
        return bean
    }
}
