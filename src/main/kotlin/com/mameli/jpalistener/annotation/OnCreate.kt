package com.mameli.jpalistener.annotation

import com.mameli.jpalistener.model.EventMode
import kotlin.reflect.KClass

/**
 * Marks a method as a handler for entity creation events.
 *
 * The annotated method will be invoked when an entity of the specified type is created and persisted to the database.
 * The method must accept a single parameter of type [com.mameli.jpalistener.model.event.EntityCreatedEvent].
 *
 * Example:
 * ```kotlin
 * @Component
 * class ProductEventHandler {
 *     @OnCreate(entityClass = Product::class)
 *     fun handleProductCreated(event: EntityCreatedEvent) {
 *         println("Product created: ${(event.entity as Product).name}")
 *     }
 * }
 * ```
 *
 * @param entityClass The entity class for which to listen for creation events
 * @param mode Controls when the handler runs relative to the transaction.
 *   Leave empty to use the globally configured default (`spring.jpalistener.default-mode`).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnCreate(val entityClass: KClass<*>, val mode: EventMode = EventMode.UNSET)
