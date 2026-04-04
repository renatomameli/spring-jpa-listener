package com.mameli.jpalistener.annotation

/**
 * Marks an entity class to be tracked for lifecycle changes (create, update, delete).
 *
 * When applied to a JPA entity, this annotation enables automatic event publishing for:
 * - Entity creation: Triggers [com.mameli.jpalistener.model.event.EntityCreatedEvent]
 * - Entity updates: Triggers [com.mameli.jpalistener.model.event.EntityUpdatedEvent] with field-level change information
 * - Entity deletion: Triggers [com.mameli.jpalistener.model.event.EntityDeletedEvent]
 *
 * The entity listener is automatically registered via a Hibernate Integrator, so no additional `@EntityListeners` annotation is needed.
 *
 * Events are fired after successful persistence operations (post-insert, post-update, post-delete).
 *
 * Example:
 * ```kotlin
 * @Entity
 * @TrackedEntity
 * class Product(
 *     @Id
 *     val id: Long? = null,
 *     val name: String,
 *     val price: BigDecimal
 * )
 * ```
 *
 * Then define event handlers in a Spring component:
 * ```kotlin
 * @Component
 * class ProductEventHandler {
 *     @OnCreate(entityClass = Product::class)
 *     fun handleCreate(event: EntityCreatedEvent) { ... }
 *
 *     @OnUpdate(entityClass = Product::class)
 *     fun handleUpdate(event: EntityUpdatedEvent) { ... }
 *
 *     @OnDelete(entityClass = Product::class)
 *     fun handleDelete(event: EntityDeletedEvent) { ... }
 * }
 * ```
 *
 * @see OnCreate
 * @see OnUpdate
 * @see OnDelete
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrackedEntity
