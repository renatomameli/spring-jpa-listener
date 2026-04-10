package com.mameli.jpalistener.annotation

import com.mameli.jpalistener.model.EventMode
import kotlin.reflect.KClass

/**
 * Marks a method as a handler for entity update events.
 *
 * The annotated method will be invoked when an entity of the specified type is updated and the changes are persisted to the database.
 * The method must accept a single parameter of type [com.mameli.jpalistener.model.event.EntityUpdatedEvent].
 *
 * The event includes a [com.mameli.jpalistener.model.ChangeSet] that contains information about which fields changed,
 * including their old and new values.
 *
 * Example:
 * ```kotlin
 * @Component
 * class ProductEventHandler {
 *     @OnUpdate(entityClass = Product::class)
 *     fun handleProductUpdated(event: EntityUpdatedEvent) {
 *         val product = event.entity as Product
 *         println("Product updated: ${product.name}")
 *         event.changeSet.changes.forEach { change ->
 *             println("  ${change.fieldName}: ${change.oldValue} -> ${change.newValue}")
 *         }
 *     }
 * }
 * ```
 *
 * @param entityClass The entity class for which to listen for update events
 * @param mode Controls when the handler runs relative to the transaction.
 *   Leave empty to use the globally configured default (`spring.jpalistener.default-mode`).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnUpdate(val entityClass: KClass<*>, val mode: EventMode = EventMode.UNSET)
