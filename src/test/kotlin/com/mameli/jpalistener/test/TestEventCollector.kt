package com.mameli.jpalistener.test

import com.mameli.jpalistener.demo.Product
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import com.mameli.jpalistener.model.event.EntityDeletedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TestEventCollector {
    
    val createdEvents = mutableListOf<EntityCreatedEvent>()
    val updatedEvents = mutableListOf<EntityUpdatedEvent>()
    val deletedEvents = mutableListOf<EntityDeletedEvent>()
    
    fun clear() {
        createdEvents.clear()
        updatedEvents.clear()
        deletedEvents.clear()
    }
    
    @EventListener
    fun onCreated(event: EntityCreatedEvent) {
        if (event.entity is Product) {
            createdEvents.add(event)
        }
    }
    
    @EventListener
    fun onUpdated(event: EntityUpdatedEvent) {
        if (event.entity is Product) {
            updatedEvents.add(event)
        }
    }
    
    @EventListener
    fun onDeleted(event: EntityDeletedEvent) {
        if (event.entity is Product) {
            deletedEvents.add(event)
        }
    }
}
