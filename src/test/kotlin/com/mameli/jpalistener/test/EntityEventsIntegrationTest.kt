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
}
