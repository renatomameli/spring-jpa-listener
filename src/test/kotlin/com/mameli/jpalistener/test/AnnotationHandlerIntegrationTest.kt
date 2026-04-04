package com.mameli.jpalistener.test

import com.mameli.jpalistener.demo.Product
import com.mameli.jpalistener.demo.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests that @OnCreate, @OnUpdate, @OnDelete annotation handlers
 * are actually discovered, registered, and invoked with correct data.
 */
@SpringBootTest(classes = [TestConfiguration::class])
@ActiveProfiles("test")
@EnableAutoConfiguration
class AnnotationHandlerIntegrationTest {
    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var handler: TestAnnotationHandler

    @BeforeEach
    fun setup() {
        handler.clear()
    }

    @Test
    fun `@OnCreate handler should be invoked with correct entity data`() {
        val product = Product()
        product.name = "Created via annotation"
        product.price = BigDecimal("42.00")
        productRepository.save(product)

        assertEquals(1, handler.creates.size)
        assertEquals("Created via annotation", handler.creates[0].name)
        assertEquals(product.id as Any, handler.creates[0].entityId)
    }

    @Test
    fun `@OnUpdate handler should be invoked with correct entity and changeSet`() {
        val product = Product()
        product.name = "Before"
        product.price = BigDecimal("10.00")
        productRepository.save(product)
        handler.clear()

        product.name = "After"
        product.price = BigDecimal("99.00")
        productRepository.save(product)

        assertEquals(1, handler.updates.size)
        val record = handler.updates[0]
        assertEquals("After", record.name)
        assertEquals(product.id as Any, record.entityId)

        // Verify changeSet contains the actual field changes
        assertTrue(record.changeSet.isFieldChanged("name"))
        assertEquals("Before", record.changeSet.getOldValue<String>("name"))
        assertEquals("After", record.changeSet.getNewValue<String>("name"))
        assertTrue(record.changeSet.isFieldChanged("price"))
    }

    @Test
    fun `@OnDelete handler should be invoked with correct entity data`() {
        val product = Product()
        product.name = "To be deleted"
        product.price = BigDecimal("5.00")
        productRepository.save(product)
        val savedId = product.id
        handler.clear()

        productRepository.delete(product)

        assertEquals(1, handler.deletes.size)
        assertEquals("To be deleted", handler.deletes[0].name)
        assertEquals(savedId as Any, handler.deletes[0].entityId)
    }

    @Test
    fun `@OnUpdate handler should receive changeSet with null transitions`() {
        val product = Product()
        product.name = "NullTest"
        product.price = BigDecimal("10.00")
        product.description = null
        productRepository.save(product)
        handler.clear()

        product.description = "Now has description"
        productRepository.save(product)

        assertEquals(1, handler.updates.size)
        val changeSet = handler.updates[0].changeSet
        assertTrue(changeSet.isFieldChanged("description"))
        assertNull(changeSet.getOldValue<String>("description"))
        assertEquals("Now has description", changeSet.getNewValue<String>("description"))
    }

    @Test
    fun `full lifecycle should invoke all three handlers in order`() {
        // Create
        val product = Product()
        product.name = "Lifecycle"
        product.price = BigDecimal("1.00")
        productRepository.save(product)

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
    }

    @Test
    fun `multiple updates should fire handler for each update`() {
        val product = Product()
        product.name = "V1"
        product.price = BigDecimal("1.00")
        productRepository.save(product)
        handler.clear()

        product.name = "V2"
        productRepository.save(product)

        product.name = "V3"
        productRepository.save(product)

        assertEquals(2, handler.updates.size)
        assertEquals("V2", handler.updates[0].name)
        assertEquals("V3", handler.updates[1].name)
    }
}
