package com.mameli.jpalistener.test

import com.mameli.jpalistener.annotation.OnCreate
import com.mameli.jpalistener.annotation.OnDelete
import com.mameli.jpalistener.annotation.OnUpdate
import com.mameli.jpalistener.demo.Product
import com.mameli.jpalistener.model.EventMode
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import com.mameli.jpalistener.model.event.EntityDeletedEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import org.springframework.stereotype.Component

/**
 * Handler that uses [EventMode.TRANSACTIONAL] mode — runs inside the active transaction.
 * Provides a configurable [shouldFail] flag so tests can trigger a rollback.
 */
@Component
class TransactionalProductHandler {

    data class CreateRecord(val entityId: Any, val name: String)
    data class UpdateRecord(val entityId: Any, val name: String)
    data class DeleteRecord(val entityId: Any, val name: String)

    val creates = mutableListOf<CreateRecord>()
    val updates = mutableListOf<UpdateRecord>()
    val deletes = mutableListOf<DeleteRecord>()

    @Volatile
    var shouldFail = false

    fun clear() {
        creates.clear()
        updates.clear()
        deletes.clear()
        shouldFail = false
    }

    @OnCreate(Product::class, mode = EventMode.TRANSACTIONAL)
    fun onProductCreated(event: EntityCreatedEvent) {
        if (shouldFail) throw RuntimeException("Transactional handler failure on create")
        val product = event.entity as Product
        creates.add(CreateRecord(event.entityId, product.name))
    }

    @OnUpdate(Product::class, mode = EventMode.TRANSACTIONAL)
    fun onProductUpdated(event: EntityUpdatedEvent) {
        if (shouldFail) throw RuntimeException("Transactional handler failure on update")
        val product = event.entity as Product
        updates.add(UpdateRecord(event.entityId, product.name))
    }

    @OnDelete(Product::class, mode = EventMode.TRANSACTIONAL)
    fun onProductDeleted(event: EntityDeletedEvent) {
        if (shouldFail) throw RuntimeException("Transactional handler failure on delete")
        val product = event.entity as Product
        deletes.add(DeleteRecord(event.entityId, product.name))
    }
}
