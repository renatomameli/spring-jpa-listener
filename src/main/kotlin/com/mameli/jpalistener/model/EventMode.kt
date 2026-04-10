package com.mameli.jpalistener.model

/**
 * Controls when entity event handlers are invoked relative to the database transaction.
 *
 * - [AFTER_COMMIT]: The handler runs **after** the transaction has committed successfully.
 *   Exceptions in the handler are logged but do **not** roll back the transaction.
 *   This is the safe "fire-and-forget" (YOLO) mode and the default for backward compatibility.
 *
 * - [TRANSACTIONAL]: The handler runs **inside** the active transaction.
 *   If the handler throws an exception, the transaction is rolled back — guaranteeing
 *   atomicity between the DB write and the side-effect.
 */
enum class EventMode {
    /** Handler runs after commit; exceptions are caught and logged. */
    AFTER_COMMIT,

    /** Handler runs inside the transaction; exceptions cause a rollback. */
    TRANSACTIONAL,

    /** Not explicitly set — the globally configured default will be used. */
    UNSET
}



