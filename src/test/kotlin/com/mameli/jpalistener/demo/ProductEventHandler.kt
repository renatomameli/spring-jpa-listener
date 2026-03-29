package com.mameli.jpalistener.demo

import com.mameli.jpalistener.annotation.OnCreate
import com.mameli.jpalistener.annotation.OnDelete
import com.mameli.jpalistener.annotation.OnUpdate
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import com.mameli.jpalistener.model.event.EntityDeletedEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProductEventHandler {

    private val logger = LoggerFactory.getLogger(ProductEventHandler::class.java)

    @OnCreate(entityClass = Product::class)
    fun handleCreate(event: EntityCreatedEvent) {
        logger.info("Product created: ${(event.entity as Product).name}")
    }

    @OnUpdate(entityClass = Product::class)
    fun handleUpdate(event: EntityUpdatedEvent) {
        val product = event.entity as Product
        logger.info("Product updated: ${product.name}")
        event.changeSet.changes.forEach { change ->
            logger.info("  Field '${change.fieldName}': ${change.oldValue} -> ${change.newValue}")
        }
    }

    @OnDelete(Product::class)
    fun handleDelete(event: EntityDeletedEvent) {
        logger.info("Product deleted: ${(event.entity as Product).name}")
    }
}
