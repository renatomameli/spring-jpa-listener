package com.mameli.jpalistener.listener

import com.mameli.jpalistener.annotation.TrackedEntity
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import com.mameli.jpalistener.model.event.EntityDeletedEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import com.mameli.jpalistener.model.ChangeSet
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate
import org.slf4j.LoggerFactory

class TrackingEntityListener {

    @PrePersist
    fun prePersist(entity: Any) {
        logger.info("PrePersist called for ${entity.javaClass.simpleName}")
        if (!isTracked(entity)) return
        val id = getEntityId(entity)
        PendingState.put(entity, entity.javaClass, id)
        logger.info("PrePersist tracked for ${entity.javaClass.simpleName}")
    }

    @PostPersist
    fun postPersist(entity: Any) {
        logger.info("PostPersist called for ${entity.javaClass.simpleName}")
        if (!isTracked(entity)) return
        val id = getEntityId(entity) ?: return
        logger.info("Publishing EntityCreatedEvent for ${entity.javaClass.simpleName}")
        EventRegistry.instance?.publish(EntityCreatedEvent(
            entityType = entity.javaClass,
            entityId = id,
            entity = entity
        ))
    }

    @PreUpdate
    fun preUpdate(entity: Any) {
        logger.info("PreUpdate called for ${entity.javaClass.simpleName}")
        if (!isTracked(entity)) return
        val id = getEntityId(entity) ?: return
        PendingState.put(entity, entity.javaClass, id)
    }

    @PostUpdate
    fun postUpdate(entity: Any) {
        logger.info("PostUpdate called for ${entity.javaClass.simpleName}")
        if (!isTracked(entity)) return
        val id = getEntityId(entity) ?: return
        logger.info("Publishing EntityUpdatedEvent for ${entity.javaClass.simpleName}")
        EventRegistry.instance?.publish(EntityUpdatedEvent(
            entityType = entity.javaClass,
            entityId = id,
            entity = entity,
            changeSet = PendingState.getChangeSet(entity) ?: ChangeSet(entity.javaClass, id, emptyList())
        ))
        PendingState.remove(entity)
    }

    @PreRemove
    fun preRemove(entity: Any) {
        logger.info("PreRemove called for ${entity.javaClass.simpleName}")
        if (!isTracked(entity)) return
        val id = getEntityId(entity) ?: return
        PendingState.put(entity, entity.javaClass, id)
    }

    @PostRemove
    fun postRemove(entity: Any) {
        logger.info("PostRemove called for ${entity.javaClass.simpleName}")
        if (!isTracked(entity)) return
        val id = getEntityId(entity) ?: return
        logger.info("Publishing EntityDeletedEvent for ${entity.javaClass.simpleName}")
        EventRegistry.instance?.publish(EntityDeletedEvent(
            entityType = entity.javaClass,
            entityId = id,
            entity = entity
        ))
        PendingState.remove(entity)
    }

    private fun isTracked(entity: Any): Boolean {
        val tracked = entity.javaClass.getAnnotation(TrackedEntity::class.java) != null
        logger.info("isTracked(${entity.javaClass.simpleName}): $tracked")
        return tracked
    }

    private fun getEntityId(entity: Any): Any? {
        val idField = entity.javaClass.declaredFields.find {
            it.getAnnotation(jakarta.persistence.Id::class.java) != null
        }
        return idField?.let {
            it.isAccessible = true
            it.get(entity)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TrackingEntityListener::class.java)
    }
}

object PendingState {
    private val state = mutableMapOf<Any, Pair<Class<*>, Any?>>()
    private val changeSets = mutableMapOf<Any, ChangeSet>()

    fun put(entity: Any, entityType: Class<*>, entityId: Any?) {
        state[entity] = entityType to entityId
    }

    fun get(entity: Any): Pair<Class<*>, Any?>? = state[entity]
    
    fun setChangeSet(entity: Any, changeSet: ChangeSet) {
        changeSets[entity] = changeSet
    }
    
    fun getChangeSet(entity: Any): ChangeSet? = changeSets[entity]

    fun remove(entity: Any) {
        state.remove(entity)
        changeSets.remove(entity)
    }
}
