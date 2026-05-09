package com.mameli.jpalistener.model

/**
 * Controls when entity event handlers are invoked relative to the database transaction.
 *
 * ## AFTER_COMMIT (default)
 * The handler runs **after** the transaction has committed successfully.
 * Exceptions in the handler are logged but do **not** roll back the transaction.
 * Safe for side-effects like logging, notifications, cache eviction.
 *
 * ## TRANSACTIONAL
 * The handler runs **inside** the active transaction.
 * If the handler throws an exception, the transaction is rolled back — guaranteeing
 * atomicity between the DB write and the side-effect.
 * Use for: atomic writes to other tables, enforcing business rules, updating read models.
 *
 * ## UNSET
 * Not explicitly set — the globally configured default (`spring.jpalistener.default-mode`) will be used.
 *
 * ### No active transaction
 * If no Spring transaction is active, both [AFTER_COMMIT] and [TRANSACTIONAL] execute immediately.
 */
enum class EventMode {
    /** Handler runs after commit; exceptions are caught and logged. */
    AFTER_COMMIT,

    /** Handler runs inside the transaction; exceptions cause a rollback. */
    TRANSACTIONAL,

    /** Not explicitly set — the globally configured default will be used. */
    UNSET
}



