package com.mameli.jpalistener.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnDelete(val entityClass: String)
