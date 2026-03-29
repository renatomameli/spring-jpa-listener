package com.mameli.jpalistener.model

data class FieldChange<T>(
    val fieldName: String,
    val oldValue: T?,
    val newValue: T?
) {
    val isChanged: Boolean = oldValue != newValue
}
