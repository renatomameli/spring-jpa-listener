# Spring JPA Listener

Automatic entity change detection and domain event publishing for Spring Data JPA.

## Features

- **Detect entity lifecycle changes**: Create, Update, Delete
- **Track field-level changes**: Get old and new values for updates
- **Two dispatch modes**: Run handlers inside the transaction or after commit
- **Flexible event handlers**: Use annotations to define handlers

## Installation

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.renatomameli:spring-jpa-listener:1.0.0")
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
import org.springframework.stereotype.Component

@Component
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

Each handler can be configured with one of two dispatch modes via the `mode` parameter:

| Mode | Timing | Exception → Rollback | Use case |
|------|--------|----------------------|----------|
| `AFTER_COMMIT` (default) | After the DB transaction commits | ❌ Exception is logged, DB stays saved | Logging, notifications, cache eviction, async side-effects |
| `TRANSACTIONAL` | Inside the active transaction | ✅ Exception propagates, transaction rolls back | Atomic side-effects, write to another table, enforce business rules |

**Default mode**: `AFTER_COMMIT`. You can change the global default in `application.yml`:

```yaml
spring:
  jpalistener:
    default-mode: TRANSACTIONAL
```

**Per-handler override**:

```kotlin
@OnCreate(entityClass = Product::class, mode = EventMode.TRANSACTIONAL)
fun handle(event: EntityCreatedEvent) { ... }
```

**No active transaction**: If no transaction is running, both modes execute immediately.

## How it works

Uses Hibernate's `EventListener` API (`PostInsertEventListener`, `PostUpdateEventListener`, `PostDeleteEventListener`)
to intercept all DB changes transparently — no manual calls in services or repositories needed.

Entities opt in via `@TrackedEntity`. The Hibernate listeners detect changes during the Hibernate flush cycle,
which always runs inside the active transaction. The event dispatch to your handler methods then follows
the configured `EventMode` (see [Transaction Handling](#transaction-handling)).

## Publishing

This library is configured for Maven Central via the Sonatype Central Portal.

Coordinates:

```text
io.github.renatomameli:spring-jpa-listener:1.0.0
```

For local releases, copy the example file and fill in your secrets:

```bash
cp maven-central-publish.properties.example maven-central-publish.properties
```

`maven-central-publish.properties` is ignored by git and should contain:

```properties
mavenCentralUsername=...
mavenCentralPassword=...
signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
```

Export the key with escaped newlines while preserving blank lines:

```bash
gpg --armor --export-secret-keys YOUR_KEY_ID | awk '{sub(/\r$/, ""); printf "%s\\n", $0;}'
```

Do not use an export command that filters blank lines, because the ASCII-armor blank line after
`-----BEGIN PGP PRIVATE KEY BLOCK-----` is required by Gradle's PGP parser.

Do not store the GPG passphrase in the file. For local releases, use the wrapper script instead:

```bash
./scripts/publish-maven-central.zsh
```

The script asks for the GPG passphrase without echoing it and passes it only to the Gradle process.

CI can alternatively provide the same keys as Gradle properties or environment-backed Gradle properties.

Manual release flow without the wrapper script:

```bash
read -rs "GPG_PASSPHRASE?GPG passphrase: "
echo
ORG_GRADLE_PROJECT_mavenCentralUsername="..." \
ORG_GRADLE_PROJECT_mavenCentralPassword="..." \
ORG_GRADLE_PROJECT_signingInMemoryKey="..." \
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$GPG_PASSPHRASE" \
./gradlew checkSigningConfiguration clean build publishToMavenCentral
unset GPG_PASSPHRASE
```

Basic verification flow:

```bash
./gradlew clean build
```

The build uses `automaticRelease = false`, so the uploaded deployment can be reviewed and released manually in the Central Portal.

## License

MIT
