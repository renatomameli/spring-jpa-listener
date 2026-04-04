package com.mameli.jpalistener.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChangeSetTest {

    private val changeSet = ChangeSet(
        entityType = String::class.java,
        entityId = 1L,
        changes = listOf(
            FieldChange("name", "old", "new"),
            FieldChange("price", 10, 20),
            FieldChange("unchanged", "same", "same")
        )
    )

    @Test
    fun `isFieldChanged should return true for changed field`() {
        assertTrue(changeSet.isFieldChanged("name"))
    }

    @Test
    fun `isFieldChanged should return false for unchanged field`() {
        assertFalse(changeSet.isFieldChanged("unchanged"))
    }

    @Test
    fun `isFieldChanged should return false for unknown field`() {
        assertFalse(changeSet.isFieldChanged("nonexistent"))
    }

    @Test
    fun `getChange should return the FieldChange`() {
        val change = changeSet.getChange("name")
        assertEquals("old", change?.oldValue)
        assertEquals("new", change?.newValue)
    }

    @Test
    fun `getChange should return null for unknown field`() {
        assertNull(changeSet.getChange("nonexistent"))
    }

    @Test
    fun `getOldValue should return old value`() {
        assertEquals("old", changeSet.getOldValue<String>("name"))
    }

    @Test
    fun `getNewValue should return new value`() {
        assertEquals("new", changeSet.getNewValue<String>("name"))
    }

    @Test
    fun `getOldValue should return null for unknown field`() {
        assertNull(changeSet.getOldValue<String>("nonexistent"))
    }

    @Test
    fun `empty changeset should have no changes`() {
        val empty = ChangeSet(String::class.java, 1L, emptyList())
        assertFalse(empty.isFieldChanged("anything"))
        assertTrue(empty.changes.isEmpty())
    }
}

