package com.mameli.jpalistener.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FieldChangeTest {
    @Test
    fun `isChanged should be true when values differ`() {
        val change = FieldChange("price", 10, 20)
        assertTrue(change.isChanged)
    }

    @Test
    fun `isChanged should be false when values are equal`() {
        val change = FieldChange("price", 10, 10)
        assertFalse(change.isChanged)
    }

    @Test
    fun `isChanged should be true when old value is null`() {
        val change = FieldChange("name", null, "new")
        assertTrue(change.isChanged)
    }

    @Test
    fun `isChanged should be true when new value is null`() {
        val change = FieldChange("name", "old", null)
        assertTrue(change.isChanged)
    }

    @Test
    fun `isChanged should be false when both values are null`() {
        val change = FieldChange("name", null, null)
        assertFalse(change.isChanged)
    }

    @Test
    fun `should store field name and values`() {
        val change = FieldChange("price", 9.99, 14.99)
        assertEquals("price", change.fieldName)
        assertEquals(9.99, change.oldValue)
        assertEquals(14.99, change.newValue)
    }
}

