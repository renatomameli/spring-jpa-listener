package com.mameli.jpalistener.config

import com.mameli.jpalistener.listener.DeleteTrackingEventListener
import com.mameli.jpalistener.listener.InsertTrackingEventListener
import com.mameli.jpalistener.listener.UpdateTrackingEventListener
import org.hibernate.boot.Metadata
import org.hibernate.boot.spi.BootstrapContext
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.integrator.spi.Integrator
import org.hibernate.service.spi.SessionFactoryServiceRegistry

/**
 * Automatically registers entity tracking event listeners for all @TrackedEntity entities.
 * Eliminates the need for manual @EntityListeners annotation on each entity class.
 */
class TrackedEntityIntegrator : Integrator {
    override fun integrate(
        metadata: Metadata,
        bootstrapContext: BootstrapContext,
        sessionFactory: SessionFactoryImplementor
    ) {
        val eventListenerRegistry = sessionFactory.serviceRegistry.getService(EventListenerRegistry::class.java)
        if (eventListenerRegistry != null) {
            eventListenerRegistry.prependListeners(EventType.POST_INSERT, InsertTrackingEventListener())
            eventListenerRegistry.prependListeners(EventType.POST_UPDATE, UpdateTrackingEventListener())
            eventListenerRegistry.prependListeners(EventType.POST_DELETE, DeleteTrackingEventListener())
        }
    }

    override fun disintegrate(
        sessionFactory: SessionFactoryImplementor,
        serviceRegistry: SessionFactoryServiceRegistry
    ) {
        // No cleanup required
    }
}
