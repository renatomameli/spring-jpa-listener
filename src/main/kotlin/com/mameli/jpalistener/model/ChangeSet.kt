package com.mameli.jpalistener.model

data class ChangeSet(
    val entityType: Class<*>,
    val entityId: Any,
    val changes: List<FieldChange<*>>
) {
    fun isFieldChanged(fieldName: String): Boolean =
        changes.any { it.fieldName == fieldName && it.isChanged }

    fun getChange(fieldName: String): FieldChange<*>? =
        changes.find { it.fieldName == fieldName }

    @Suppress("UNCHECKED_CAST")
    fun <T> getOldValue(fieldName: String): T? =
        changes.find { it.fieldName == fieldName }?.oldValue as? T

    @Suppress("UNCHECKED_CAST")
    fun <T> getNewValue(fieldName: String): T? =
        changes.find { it.fieldName == fieldName }?.newValue as? T
}
