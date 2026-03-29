package com.mameli.jpalistener.handler

import com.mameli.jpalistener.annotation.OnCreate
import com.mameli.jpalistener.annotation.OnDelete
import com.mameli.jpalistener.annotation.OnUpdate
import com.mameli.jpalistener.listener.EventRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

class AnnotationHandlerScanner(
    private val applicationContext: ApplicationContext,
    private val eventRegistry: EventRegistry
) : ApplicationListener<ContextRefreshedEvent> {

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        scanAndRegister()
    }

    fun scanAndRegister() {
        val annotatedBeans = getAnnotatedBeans()
        
        for ((beanName, beanClass) in annotatedBeans) {
            try {
                val bean = applicationContext.getBean(beanName)
                
                for (method in beanClass.declaredMethods) {
                    val onCreate: OnCreate? = AnnotationUtils.findAnnotation(method, OnCreate::class.java)
                    if (onCreate != null) {
                        val entityClass = onCreate.entityClass.java
                        eventRegistry.registerHandler(entityClass.name, bean, method)
                        logger.info("Registered @OnCreate: ${beanClass.simpleName}.${method.name} -> ${entityClass.simpleName}")
                    }

                    val onUpdate: OnUpdate? = AnnotationUtils.findAnnotation(method, OnUpdate::class.java)
                    if (onUpdate != null) {
                        val entityClass = onUpdate.entityClass.java
                        eventRegistry.registerHandler(entityClass.name, bean, method)
                        logger.info("Registered @OnUpdate: ${beanClass.simpleName}.${method.name} -> ${entityClass.simpleName}")
                    }

                    val onDelete: OnDelete? = AnnotationUtils.findAnnotation(method, OnDelete::class.java)
                    if (onDelete != null) {
                        val entityClass = onDelete.entityClass.java
                        eventRegistry.registerHandler(entityClass.name, bean, method)
                        logger.info("Registered @OnDelete: ${beanClass.simpleName}.${method.name} -> ${entityClass.simpleName}")
                    }
                }
            } catch (e: Exception) {
                logger.debug("Could not process bean $beanName", e)
            }
        }
    }

    private fun getAnnotatedBeans(): Map<String, Class<*>> {
        val result = mutableMapOf<String, Class<*>>()
        val annotationTypes = setOf(
            Service::class.java, 
            Component::class.java, 
            Repository::class.java
        )
        
        for (beanName in applicationContext.beanDefinitionNames) {
            try {
                val beanType = applicationContext.getType(beanName) ?: continue
                for (annotationType in annotationTypes) {
                    if (beanType.getAnnotation(annotationType) != null) {
                        result[beanName] = beanType
                        break
                    }
                }
            } catch (e: BeansException) {
                // Skip beans that can't be accessed
            }
        }
        return result
    }

    private val logger = LoggerFactory.getLogger(AnnotationHandlerScanner::class.java)
}
