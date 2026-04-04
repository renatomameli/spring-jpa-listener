package com.mameli.jpalistener.test

import com.mameli.jpalistener.demo.Product
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

@SpringBootTest(classes = [TestConfiguration::class])
@ActiveProfiles("test")
@EnableAutoConfiguration
class EntityEventsIntegrationTest {

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var eventCollector: TestEventCollector

    @BeforeEach
    fun setup() {
        eventCollector.clear()
    }

    @Test
    fun `should fire onCreate event when saving new entity`() {
        val product = Product()
        product.name = "Test Product"
        product.price = BigDecimal("99.99")
        productRepository.save(product)
        
        println("Created events: ${eventCollector.createdEvents.size}")
        
        assert(eventCollector.createdEvents.size == 1) { 
            "Expected 1 create event, got ${eventCollector.createdEvents.size}" 
        }
        val event = eventCollector.createdEvents[0]
        val entity = event.entity as Product
        assert(entity.name == "Test Product")
    }

    @Test
    fun `should fire onDelete event when deleting entity`() {
        val product = Product()
        product.name = "To Delete"
        product.price = BigDecimal("50.00")
        productRepository.save(product)
        
        productRepository.delete(product)
        
        println("Deleted events: ${eventCollector.deletedEvents.size}")
        
        assert(eventCollector.deletedEvents.size == 1) { 
            "Expected 1 delete event, got ${eventCollector.deletedEvents.size}" 
        }
    }

    @Test
    fun `should fire onUpdate event when modifying entity`() {
        val product = Product()
        product.name = "Original"
        product.price = BigDecimal("10.00")
        productRepository.save(product)
        eventCollector.clear()
        
        product.name = "Updated"
        product.price = BigDecimal("20.00")
        productRepository.save(product)
        
        println("Updated events: ${eventCollector.updatedEvents.size}")
        
        assert(eventCollector.updatedEvents.size == 1) { 
            "Expected 1 update event, got ${eventCollector.updatedEvents.size}" 
        }
    }

    @Test
    fun `should include correct changeSet on update event`() {
        val product = Product()
        product.name = "Original"
        product.price = BigDecimal("10.00")
        productRepository.save(product)
        eventCollector.clear()

        product.name = "Updated"
        product.price = BigDecimal("20.00")
        productRepository.save(product)

        assertEquals(1, eventCollector.updatedEvents.size)
        val changeSet = eventCollector.updatedEvents[0].changeSet
        assertTrue(changeSet.isFieldChanged("name"))
        assertTrue(changeSet.isFieldChanged("price"))
        assertEquals("Original", changeSet.getOldValue<String>("name"))
        assertEquals("Updated", changeSet.getNewValue<String>("name"))
    }

    @Test
    fun `should handle null field changes`() {
        val product = Product()
        product.name = "WithDesc"
        product.price = BigDecimal("10.00")
        product.description = "Some description"
        productRepository.save(product)
        eventCollector.clear()

        product.description = null
        productRepository.save(product)

        assertEquals(1, eventCollector.updatedEvents.size)
        val changeSet = eventCollector.updatedEvents[0].changeSet
        assertTrue(changeSet.isFieldChanged("description"))
        assertEquals("Some description", changeSet.getOldValue<String>("description"))
        assertNull(changeSet.getNewValue<String>("description"))
    }

    @Test
    fun `should fire create and delete events for same entity`() {
        val product = Product()
        product.name = "Lifecycle"
        product.price = BigDecimal("1.00")
        productRepository.save(product)
        productRepository.delete(product)

        assertEquals(1, eventCollector.createdEvents.size)
        assertEquals(1, eventCollector.deletedEvents.size)
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
}
