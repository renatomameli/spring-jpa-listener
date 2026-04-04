package com.mameli.jpalistener.listener

import com.mameli.jpalistener.model.event.EntityCreatedEvent
import org.hibernate.event.spi.PostInsertEvent
import org.hibernate.event.spi.PostInsertEventListener
import org.hibernate.persister.entity.EntityPersister

/**
 * Publishes [EntityCreatedEvent] when a @TrackedEntity is inserted.
 */
class InsertTrackingEventListener : PostInsertEventListener {

    override fun onPostInsert(event: PostInsertEvent) {
        if (!isTrackedEntity(event.entity)) return
        val id = getEntityId(event.entity) ?: return

        EventRegistry.instance?.publish(EntityCreatedEvent(event.entity.javaClass, id, event.entity))
    }

    override fun requiresPostCommitHandling(persister: EntityPersister): Boolean = false
}

