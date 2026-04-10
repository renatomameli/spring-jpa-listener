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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Configuration
@ComponentScan(basePackages = ["com.mameli.jpalistener"])
@EntityScan(basePackages = ["com.mameli.jpalistener.demo"])
open class TransactionalTestConfiguration

/**
 * Integration tests for [com.mameli.jpalistener.model.EventMode.TRANSACTIONAL] mode.
 *
 * Verifies that:
 * - Handlers run inside the transaction (can observe data)
 * - Throwing an exception in a TRANSACTIONAL handler causes the transaction to roll back
 * - The entity is NOT persisted when the handler fails
 */
@SpringBootTest(classes = [TransactionalTestConfiguration::class])
@ActiveProfiles("test")
@EnableAutoConfiguration
class TransactionalModeIntegrationTest {

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var transactionalHandler: TransactionalProductHandler

    @BeforeEach
    fun setup() {
        transactionalHandler.clear()
        productRepository.deleteAll()
    }

    @Test
    fun `transactional handler should be invoked on create`() {
        val product = Product()
        product.name = "TX Product"
        product.price = BigDecimal("42.00")
        productRepository.save(product)

        assertEquals(1, transactionalHandler.creates.size)
        assertEquals("TX Product", transactionalHandler.creates[0].name)
    }

    @Test
    fun `transactional handler should be invoked on update`() {
        val product = Product()
        product.name = "Original"
        product.price = BigDecimal("10.00")
        productRepository.save(product)
        transactionalHandler.clear()

        product.name = "Updated"
        productRepository.save(product)

        assertEquals(1, transactionalHandler.updates.size)
        assertEquals("Updated", transactionalHandler.updates[0].name)
    }

    @Test
    fun `transactional handler should be invoked on delete`() {
        val product = Product()
        product.name = "To Delete"
        product.price = BigDecimal("5.00")
        productRepository.save(product)
        transactionalHandler.clear()

        productRepository.delete(product)

        assertEquals(1, transactionalHandler.deletes.size)
        assertEquals("To Delete", transactionalHandler.deletes[0].name)
    }

    @Test
    fun `transactional handler exception should roll back create`() {
        transactionalHandler.shouldFail = true

        assertFailsWith<RuntimeException>("Transactional handler failure on create") {
            val product = Product()
            product.name = "Should Not Persist"
            product.price = BigDecimal("99.00")
            productRepository.save(product)
        }

        // Entity should NOT be in the database because the transaction was rolled back
        assertEquals(0, productRepository.count())
    }

    @Test
    fun `transactional handler exception should roll back update`() {
        val product = Product()
        product.name = "Original"
        product.price = BigDecimal("10.00")
        productRepository.save(product)
        transactionalHandler.clear()

        transactionalHandler.shouldFail = true

        assertFailsWith<RuntimeException>("Transactional handler failure on update") {
            product.name = "Should Stay Original"
            productRepository.save(product)
        }

        // The name should still be "Original" because the update transaction was rolled back
        val reloaded = productRepository.findById(product.id!!).orElse(null)
        assertTrue(reloaded != null, "Product should still exist")
        assertEquals("Original", reloaded.name)
    }

    @Test
    fun `transactional handler exception should roll back delete`() {
        val product = Product()
        product.name = "Should Survive"
        product.price = BigDecimal("10.00")
        productRepository.save(product)
        val savedId = product.id!!
        transactionalHandler.clear()

        transactionalHandler.shouldFail = true

        assertFailsWith<RuntimeException>("Transactional handler failure on delete") {
            productRepository.delete(product)
        }

        // The entity should still be in the database because the delete transaction was rolled back
        assertTrue(productRepository.findById(savedId).isPresent, "Product should still exist after rollback")
    }
}

