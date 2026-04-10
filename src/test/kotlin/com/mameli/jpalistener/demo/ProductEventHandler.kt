package com.mameli.jpalistener.demo

import com.mameli.jpalistener.annotation.OnCreate
import com.mameli.jpalistener.annotation.OnDelete
import com.mameli.jpalistener.annotation.OnUpdate
import com.mameli.jpalistener.model.ChangeSet
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import com.mameli.jpalistener.model.event.EntityDeletedEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import org.springframework.stereotype.Component

/**
 * Test handler that uses @OnCreate/@OnUpdate/@OnDelete annotations
 * to verify the annotation-based handler registration and invocation path.
 */
@Component
class ProductEventHandler {

    data class CreateRecord(val entityId: Any, val name: String)
    data class UpdateRecord(val entityId: Any, val name: String, val changeSet: ChangeSet)
    data class DeleteRecord(val entityId: Any, val name: String)

    val creates = mutableListOf<CreateRecord>()
    val updates = mutableListOf<UpdateRecord>()
    val deletes = mutableListOf<DeleteRecord>()

    fun clear() {
        creates.clear()
        updates.clear()
        deletes.clear()
    }

    @OnCreate(Product::class)
    fun onProductCreated(event: EntityCreatedEvent) {
        val product = event.entity as Product
        creates.add(CreateRecord(event.entityId, product.name))
    }

    @OnUpdate(Product::class)
    fun onProductUpdated(event: EntityUpdatedEvent) {
        val product = event.entity as Product
        updates.add(UpdateRecord(event.entityId, product.name, event.changeSet))
    }

    @OnDelete(Product::class)
    fun onProductDeleted(event: EntityDeletedEvent) {
        val product = event.entity as Product
        deletes.add(DeleteRecord(event.entityId, product.name))
    }
}