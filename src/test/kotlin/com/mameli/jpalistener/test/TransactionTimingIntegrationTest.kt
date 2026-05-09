package com.mameli.jpalistener.test

import com.mameli.jpalistener.annotation.OnCreate
import com.mameli.jpalistener.demo.Product
import com.mameli.jpalistener.demo.ProductRepository
import com.mameli.jpalistener.model.EventMode
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Configuration
@ComponentScan(basePackages = ["com.mameli.jpalistener"])
@EntityScan(basePackages = ["com.mameli.jpalistener.demo"])
open class TimingTestConfig

/**
 * Proves the timing difference between [EventMode.AFTER_COMMIT] and [EventMode.TRANSACTIONAL].
 *
 * Uses [TransactionTemplate] to manually control the transaction boundary so we can
 * check handler state before and after commit.
 */
@SpringBootTest(classes = [TimingTestConfig::class])
@ActiveProfiles("test")
@EnableAutoConfiguration
class TransactionTimingIntegrationTest {

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var afterCommitTimingHandler: AfterCommitTimingHandler

    @Autowired
    lateinit var transactionalTimingHandler: TransactionalTimingHandler

    @Test
    fun `AFTER_COMMIT handler should fire AFTER the transaction commits`() {
        afterCommitTimingHandler.invoked = false

        transactionTemplate.execute {
            val product = product()
            productRepository.save(product)
            afterCommitTimingHandler.invoked
        }.let { calledInsideTx ->
            assertFalse(calledInsideTx, "AFTER_COMMIT handler should NOT fire during the TX")
        }

        assertTrue(afterCommitTimingHandler.invoked, "AFTER_COMMIT handler should fire after commit")
    }

    @Test
    fun `TRANSACTIONAL handler should fire INSIDE the transaction`() {
        transactionalTimingHandler.invoked = false

        transactionTemplate.execute {
            val product = product()
            productRepository.save(product)
            transactionalTimingHandler.invoked
        }.let { calledInsideTx ->
            assertTrue(calledInsideTx, "TRANSACTIONAL handler should fire during the TX")
        }

        assertTrue(transactionalTimingHandler.invoked, "TRANSACTIONAL handler should be recorded")
    }

    private fun product() = Product().apply {
        name = "Timing-Test"
        price = BigDecimal.TEN
    }
}

@Component
class AfterCommitTimingHandler {
    @Volatile
    var invoked = false

    @OnCreate(Product::class)
    fun handle(event: EntityCreatedEvent) {
        invoked = true
    }
}

@Component
class TransactionalTimingHandler {
    @Volatile
    var invoked = false

    @OnCreate(Product::class, mode = EventMode.TRANSACTIONAL)
    fun handle(event: EntityCreatedEvent) {
        invoked = true
    }
}
