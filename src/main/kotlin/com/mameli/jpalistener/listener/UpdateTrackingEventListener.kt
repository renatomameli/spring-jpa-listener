package com.mameli.jpalistener.listener

import com.mameli.jpalistener.model.ChangeSet
import com.mameli.jpalistener.model.FieldChange
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import org.hibernate.event.spi.PostUpdateEvent
import org.hibernate.event.spi.PostUpdateEventListener
import org.hibernate.persister.entity.EntityPersister

/**
 * Publishes [EntityUpdatedEvent] with a [ChangeSet] when a @TrackedEntity is updated.
 */
class UpdateTrackingEventListener : PostUpdateEventListener {

    override fun onPostUpdate(event: PostUpdateEvent) {
        if (!isTrackedEntity(event.entity)) return
        val entity = event.entity
        val id = getEntityId(entity) ?: return

        val event = EntityUpdatedEvent(entity.javaClass, id, entity, computeChangeSet(event))
        EventRegistry.instance?.publish(event)
    }

    override fun requiresPostCommitHandling(persister: EntityPersister): Boolean = false

    private fun computeChangeSet(event: PostUpdateEvent): ChangeSet {
        val entity = event.entity
        val propertyNames = event.persister.propertyNames
        val oldState = event.oldState
        val newState = event.state
        val entityId = event.id

        if (oldState == null || entityId == null) {
            return ChangeSet(entity.javaClass, entityId ?: 0, emptyList())
        }

        val changes = propertyNames.indices
            .filter { oldState[it] != newState[it] }
            .map { i ->
                FieldChange(propertyNames[i], oldState[i], newState[i])
            }

        return ChangeSet(entity.javaClass, entityId, changes)
    }
}

