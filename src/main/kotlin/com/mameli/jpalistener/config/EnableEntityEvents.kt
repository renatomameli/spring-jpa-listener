package com.mameli.jpalistener.config

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@Import(EntityEventsAutoConfiguration::class)
annotation class EnableEntityEvents(
    val async: Boolean = false
)
