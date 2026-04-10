package com.mameli.jpalistener.config

import com.mameli.jpalistener.model.EventMode
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the JPA listener library.
 *
 * Can be set in `application.yml`:
 * ```yaml
 * spring:
 *   jpalistener:
 *     default-mode: AFTER_COMMIT   # or TRANSACTIONAL
 * ```
 *
 * @property defaultMode the default [EventMode] for all handlers that do not
 *   explicitly specify a mode in their annotation. Defaults to [EventMode.AFTER_COMMIT].
 */
@ConfigurationProperties(prefix = "spring.jpalistener")
data class JpaListenerProperties(
    val defaultMode: EventMode = EventMode.AFTER_COMMIT
)


