package com.mameli.jpalistener.test

import com.mameli.jpalistener.annotation.OnCreate
import com.mameli.jpalistener.demo.Product
import com.mameli.jpalistener.demo.ProductRepository
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Configuration
@ComponentScan(basePackages = ["com.mameli.jpalistener"])
@EntityScan(basePackages = ["com.mameli.jpalistener.demo"])
open class DefaultModeTestConfig

/**
 * Verifies that the global [spring.jpalistener.default-mode] property is respected
 * by handlers that do not specify an explicit mode.
 *
 * With `default-mode=TRANSACTIONAL`, a handler without `mode` should run inside
 * the transaction — an exception must roll back the save.
 */
@SpringBootTest(classes = [DefaultModeTestConfig::class])
@ActiveProfiles("test")
@EnableAutoConfiguration
@TestPropertySource(properties = ["spring.jpalistener.default-mode=TRANSACTIONAL"])
class DefaultModeIntegrationTest {

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var defaultModeHandler: DefaultModeHandler

    @AfterEach
    fun cleanup() {
        defaultModeHandler.fail = false
    }

    @Test
    fun `handler without explicit mode uses global TRANSACTIONAL default`() {
        defaultModeHandler.fail = true

        assertFailsWith<RuntimeException>("fail in handler") {
            val product = Product()
            product.name = "ShouldRollBack"
            product.price = BigDecimal("10.00")
            productRepository.save(product)
        }

        assertEquals(0, productRepository.count())
    }
}

@Component
class DefaultModeHandler {
    @Volatile
    var fail = false

    @OnCreate(Product::class)
    fun handle(event: EntityCreatedEvent) {
        if (fail) throw RuntimeException("fail in handler")
    }
}
