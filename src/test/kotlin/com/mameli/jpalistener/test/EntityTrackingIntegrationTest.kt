package com.mameli.jpalistener.test

import com.mameli.jpalistener.demo.Product
import com.mameli.jpalistener.demo.ProductEventHandler
import com.mameli.jpalistener.demo.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Configuration
@ComponentScan(basePackages = ["com.mameli.jpalistener"])
@EntityScan(basePackages = ["com.mameli.jpalistener.demo"])
open class TestConfiguration

/**
 * Consolidated integration tests for the entity tracking system.
 *
 * Each test verifies both dispatch paths simultaneously:
 * - **Spring @EventListener** path via [TestEventCollector]
 * - **@OnCreate / @OnUpdate / @OnDelete annotation** path via [ProductEventHandler]
 */
@SpringBootTest(classes = [TestConfiguration::class])
@ActiveProfiles("test")
@EnableAutoConfiguration
class EntityTrackingIntegrationTest {

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var eventCollector: TestEventCollector

    @Autowired
    lateinit var handler: ProductEventHandler

    @BeforeEach
    fun setup() {
        eventCollector.clear()
        handler.clear()
    }

    @Test
    fun `should fire create event on both paths`() {
        val product = Product()
        product.name = "Test Product"
        product.price = BigDecimal("99.99")
        productRepository.save(product)

        // Spring @EventListener path
        assertEquals(1, eventCollector.createdEvents.size)
        val event = eventCollector.createdEvents[0]
        assertEquals("Test Product", (event.entity as Product).name)

        // @OnCreate annotation path
        assertEquals(1, handler.creates.size)
        assertEquals("Test Product", handler.creates[0].name)
        assertEquals(product.id as Any, handler.creates[0].entityId)
    }

    @Test
    fun `should fire delete event on both paths`() {
        val product = Product()
        product.name = "To Delete"
        product.price = BigDecimal("50.00")
        productRepository.save(product)
        val savedId = product.id
        eventCollector.clear()
        handler.clear()

        productRepository.delete(product)

        // Spring @EventListener path
        assertEquals(1, eventCollector.deletedEvents.size)

        // @OnDelete annotation path
        assertEquals(1, handler.deletes.size)
        assertEquals("To Delete", handler.deletes[0].name)
        assertEquals(savedId as Any, handler.deletes[0].entityId)
    }

    @Test
    fun `should fire update event with correct changeSet on both paths`() {
        val product = Product()
        product.name = "Original"
        product.price = BigDecimal("10.00")
        productRepository.save(product)
        eventCollector.clear()
        handler.clear()

        product.name = "Updated"
        product.price = BigDecimal("20.00")
        productRepository.save(product)

        // Spring @EventListener path
        assertEquals(1, eventCollector.updatedEvents.size)
        val eventChangeSet = eventCollector.updatedEvents[0].changeSet
        assertTrue(eventChangeSet.isFieldChanged("name"))
        assertTrue(eventChangeSet.isFieldChanged("price"))
        assertEquals("Original", eventChangeSet.getOldValue<String>("name"))
        assertEquals("Updated", eventChangeSet.getNewValue<String>("name"))

        // @OnUpdate annotation path
        assertEquals(1, handler.updates.size)
        val record = handler.updates[0]
        assertEquals("Updated", record.name)
        assertEquals(product.id as Any, record.entityId)
        assertTrue(record.changeSet.isFieldChanged("name"))
        assertEquals("Original", record.changeSet.getOldValue<String>("name"))
        assertEquals("Updated", record.changeSet.getNewValue<String>("name"))
        assertTrue(record.changeSet.isFieldChanged("price"))
    }

    @Test
    fun `should handle null-to-value transition in changeSet`() {
        val product = Product()
        product.name = "NullTest"
        product.price = BigDecimal("10.00")
        product.description = null
        productRepository.save(product)
        eventCollector.clear()
        handler.clear()

        product.description = "Now has description"
        productRepository.save(product)

        // Spring @EventListener path
        assertEquals(1, eventCollector.updatedEvents.size)
        val eventChangeSet = eventCollector.updatedEvents[0].changeSet
        assertTrue(eventChangeSet.isFieldChanged("description"))
        assertNull(eventChangeSet.getOldValue<String>("description"))
        assertEquals("Now has description", eventChangeSet.getNewValue<String>("description"))

        // @OnUpdate annotation path
        assertEquals(1, handler.updates.size)
        val handlerChangeSet = handler.updates[0].changeSet
        assertTrue(handlerChangeSet.isFieldChanged("description"))
        assertNull(handlerChangeSet.getOldValue<String>("description"))
        assertEquals("Now has description", handlerChangeSet.getNewValue<String>("description"))
    }

    @Test
    fun `should handle value-to-null transition in changeSet`() {
        val product = Product()
        product.name = "WithDesc"
        product.price = BigDecimal("10.00")
        product.description = "Some description"
        productRepository.save(product)
        eventCollector.clear()
        handler.clear()

        product.description = null
        productRepository.save(product)

        assertEquals(1, eventCollector.updatedEvents.size)
        val changeSet = eventCollector.updatedEvents[0].changeSet
        assertTrue(changeSet.isFieldChanged("description"))
        assertEquals("Some description", changeSet.getOldValue<String>("description"))
        assertNull(changeSet.getNewValue<String>("description"))
    }

    @Test
    fun `should not include unchanged fields in changeSet`() {
        val product = Product()
        product.name = "Original"
        product.price = BigDecimal("10.00")
        product.description = "Keep this"
        productRepository.save(product)
        eventCollector.clear()

        product.name = "Changed"
        productRepository.save(product)

        assertEquals(1, eventCollector.updatedEvents.size)
        val changeSet = eventCollector.updatedEvents[0].changeSet
        assertTrue(changeSet.isFieldChanged("name"))
        assertNull(changeSet.getChange("description"))
    }

    @Test
    fun `full lifecycle should fire create, update, and delete in order`() {
        // Create
        val product = Product()
        product.name = "Lifecycle"
        product.price = BigDecimal("1.00")
        productRepository.save(product)

        assertEquals(1, eventCollector.createdEvents.size)
        assertEquals(1, handler.creates.size)
        assertEquals(0, handler.updates.size)
        assertEquals(0, handler.deletes.size)

        // Update
        product.name = "Updated"
        productRepository.save(product)

        assertEquals(1, handler.creates.size)
        assertEquals(1, handler.updates.size)
        assertEquals(0, handler.deletes.size)

        // Delete
        productRepository.delete(product)

        assertEquals(1, handler.creates.size)
        assertEquals(1, handler.updates.size)
        assertEquals(1, handler.deletes.size)
        assertEquals(1, eventCollector.deletedEvents.size)
    }

    @Test
    fun `multiple updates should fire handler for each update`() {
        val product = Product()
        product.name = "V1"
        product.price = BigDecimal("1.00")
        productRepository.save(product)
        handler.clear()
        eventCollector.clear()

        product.name = "V2"
        productRepository.save(product)

        product.name = "V3"
        productRepository.save(product)

        assertEquals(2, handler.updates.size)
        assertEquals("V2", handler.updates[0].name)
        assertEquals("V3", handler.updates[1].name)

        assertEquals(2, eventCollector.updatedEvents.size)
    }
}

