package com.mameli.jpalistener.model.event

import com.mameli.jpalistener.model.ChangeSet

sealed class EntityEvent(
    open val entityType: Class<*>,
    open val entityId: Any,
    open val timestamp: Long = System.currentTimeMillis()
) {
    abstract val entity: Any
}

data class EntityCreatedEvent(
    override val entityType: Class<*>,
    override val entityId: Any,
    override val entity: Any,
    override val timestamp: Long = System.currentTimeMillis(),
) : EntityEvent(entityType, entityId, timestamp)

data class EntityUpdatedEvent(
    override val entityType: Class<*>,
    override val entityId: Any,
    override val entity: Any,
    val changeSet: ChangeSet,
    override val timestamp: Long = System.currentTimeMillis(),
) : EntityEvent(entityType, entityId, timestamp)

data class EntityDeletedEvent(
    override val entityType: Class<*>,
    override val entityId: Any,
    override val entity: Any,
    override val timestamp: Long = System.currentTimeMillis(),
) : EntityEvent(entityType, entityId, timestamp)
