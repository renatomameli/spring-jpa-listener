package com.mameli.jpalistener.listener

import com.mameli.jpalistener.model.event.EntityDeletedEvent
import org.hibernate.event.spi.PostDeleteEvent
import org.hibernate.event.spi.PostDeleteEventListener
import org.hibernate.persister.entity.EntityPersister

/**
 * Publishes [EntityDeletedEvent] when a @TrackedEntity is deleted.
 */
class DeleteTrackingEventListener : PostDeleteEventListener {

    override fun onPostDelete(event: PostDeleteEvent) {
        if (!isTrackedEntity(event.entity)) return
        val id = getEntityId(event.entity) ?: return

        EventRegistry.instance?.publish(EntityDeletedEvent(
            entityType = event.entity.javaClass,
            entityId = id,
            entity = event.entity
        ))
    }

    override fun requiresPostCommitHandling(persister: EntityPersister): Boolean = false
}

