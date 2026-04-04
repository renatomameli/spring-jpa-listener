# Spring JPA Listener

Automatic entity change detection and domain event publishing for Spring Data JPA.

## Features

- **Detect entity lifecycle changes**: Create, Update, Delete
- **Track field-level changes**: Get old and new values for updates
- **Publish Spring events**: After transaction commit
- **Flexible event handlers**: Use annotations to define handlers

## Installation

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.mameli:spring-jpa-listener:1.0.0")
}
```

## Usage

### 1. Mark Entity as Tracked

```kotlin
import com.mameli.jpalistener.annotation.TrackedEntity
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
@TrackedEntity
class Product(
    @Id
    val id: Long? = null,
    val name: String,
    val price: BigDecimal
)
```

> **Note**: The `@TrackedEntity` annotation automatically registers the entity listener with Hibernate. No additional `@EntityListeners` annotation is needed!

### 2. Create Event Handler

```kotlin
import com.mameli.jpalistener.annotation.OnCreate
import com.mameli.jpalistener.annotation.OnUpdate
import com.mameli.jpalistener.annotation.OnDelete
import com.mameli.jpalistener.model.event.EntityCreatedEvent
import com.mameli.jpalistener.model.event.EntityUpdatedEvent
import com.mameli.jpalistener.model.event.EntityDeletedEvent
import org.springframework.stereotype.Service

@Service
class ProductEventHandler {

    @OnCreate(entityClass = Product::class)
    fun handleCreate(event: EntityCreatedEvent) {
        val product = event.entity as Product
        println("Product created: ${product.name}")
    }

    @OnUpdate(entityClass = Product::class)
    fun handleUpdate(event: EntityUpdatedEvent) {
        val product = event.entity as Product
        println("Product updated: ${product.name}")
        
        // Access field changes
        event.changeSet.changes.forEach { change ->
            println("  ${change.fieldName}: ${change.oldValue} -> ${change.newValue}")
        }
    }

    @OnDelete(entityClass = Product::class)
    fun handleDelete(event: EntityDeletedEvent) {
        val product = event.entity as Product
        println("Product deleted: ${product.name}")
    }
}
```

## Events

### EntityCreatedEvent

```kotlin
data class EntityCreatedEvent(
    val entityType: Class<*>,
    val entityId: Any,
    val entity: Any
)
```

### EntityUpdatedEvent

```kotlin
data class EntityUpdatedEvent(
    val entityType: Class<*>,
    val entityId: Any,
    val entity: Any,
    val changeSet: ChangeSet
)
```

### EntityDeletedEvent

```kotlin
data class EntityDeletedEvent(
    val entityType: Class<*>,
    val entityId: Any,
    val entity: Any
)
```

### ChangeSet

```kotlin
data class ChangeSet(
    val entityType: Class<*>,
    val entityId: Any,
    val changes: List<FieldChange<*>>
)

data class FieldChange<T>(
    val fieldName: String,
    val oldValue: T?,
    val newValue: T?
)
```

## Spring Events

All events are also published as Spring `PayloadApplicationEvent`:

```kotlin
@EventListener
fun handleCreated(event: PayloadApplicationEvent<EntityCreatedEvent>) {
    println("Spring event received: ${event.payload}")
}
```

## Async Handling

Events can be processed asynchronously by using `@Async` on the handler method:

```kotlin
@Service
class AsyncHandler {
    @Async
    @OnCreate(entityClass = Product::class)
    fun handleCreate(event: EntityCreatedEvent) {
        // Runs in separate thread
    }
}
```

## Transaction Handling

Handlers are executed after the transaction commits by default. If no transaction is active, handlers execute immediately.

## License

MIT
