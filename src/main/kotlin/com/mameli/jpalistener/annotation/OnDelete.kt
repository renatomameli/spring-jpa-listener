package com.mameli.jpalistener.annotation

import kotlin.reflect.KClass

/**
 * Marks a method as a handler for entity deletion events.
 *
 * The annotated method will be invoked when an entity of the specified type is deleted from the database.
 * The method must accept a single parameter of type [com.mameli.jpalistener.model.event.EntityDeletedEvent].
 *
 * Note: The event fires after the entity has been successfully deleted from the database.
 *
 * Example:
 * ```kotlin
 * @Component
 * class ProductEventHandler {
 *     @OnDelete(entityClass = Product::class)
 *     fun handleProductDeleted(event: EntityDeletedEvent) {
 *         val product = event.entity as Product
 *         println("Product deleted: ${product.name}")
 *     }
 * }
 * ```
 *
 * @param entityClass The entity class for which to listen for deletion events
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnDelete(val entityClass: KClass<*>)
