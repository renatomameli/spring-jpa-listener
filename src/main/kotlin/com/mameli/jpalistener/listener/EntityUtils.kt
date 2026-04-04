package com.mameli.jpalistener.listener

import com.mameli.jpalistener.annotation.TrackedEntity

internal fun isTrackedEntity(entity: Any): Boolean =
    entity.javaClass.isAnnotationPresent(TrackedEntity::class.java)

internal fun getEntityId(entity: Any): Any? {
    val idField = entity.javaClass.declaredFields.find {
        it.getAnnotation(jakarta.persistence.Id::class.java) != null
    }
    return idField?.let {
        it.isAccessible = true
        it.get(entity)
    }
}

