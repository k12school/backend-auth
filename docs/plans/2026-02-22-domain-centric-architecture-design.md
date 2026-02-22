# Domain-Centric Architecture Design

**Date:** 2026-02-22
**Status:** Approved
**Scope:** Company-wide template for Java backend applications

## Executive Summary

This document defines a domain-centric architecture pattern for company-wide adoption. The architecture is based on Ports and Adapters (Hexagonal), Railway Oriented Programming (ROP), and Event Sourcing principles. All business logic resides in the domain layer, with no exceptions for error handling - only `Result<T, E>` types.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Package Structure](#package-structure)
3. [Request Processing Flow](#request-processing-flow)
4. [Error Handling](#error-handling)
5. [Testing Strategy](#testing-strategy)
6. [Cross-Cutting Concerns](#cross-cutting-concerns)
7. [Adoption Guidelines](#adoption-guidelines)

---

## Architecture Overview

### High-Level Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                         DRIVING ADAPTERS                         │
│  REST Controllers | Message Consumers | Scheduled Jobs          │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                         INPUT LAYER                              │
│  - DTO validation (format, required fields)                      │
│  - DTO → Command conversion                                      │
│  - Pre-condition checks                                          │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION SERVICES                          │
│  - CommandHandler (simple flows)                                 │
│  - Functional Pipeline (complex flows)                           │
│  - Saga Orchestrator (multi-aggregate operations)                │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DOMAIN PORTS                               │
│  Input Ports: Repository interfaces                              │
│  Output Ports: EventStore, ProjectionUpdater, SideEffectTrigger  │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DOMAIN CORE                                │
│  - Entities (process commands → return events)                   │
│  - Reconstructors (apply events → return entity)                 │
│  - Commands, Events, Errors (sealed interfaces)                  │
│  - ZERO dependencies on outer layers                             │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DRIVEN ADAPTERS                            │
│  - EventStore (PostgreSQL event log)                             │
│  - ProjectionUpdater (read models)                               │
│  - SideEffectTrigger (async operations)                          │
└─────────────────────────────────────────────────────────────────┘
```

### Key Principles

1. **Domain Centric**: All business logic lives in domain layer
2. **Ports & Adapters**: Domain defines ports, infrastructure implements adapters
3. **ROP Only**: No exceptions for business logic, always use `Result<T, E>`
4. **Event Sourcing**: Entities are projections of their event stream
5. **Immutable**: All entities and value objects are immutable records
6. **Sealed Types**: Commands, events, errors use sealed interfaces for type safety

---

## Package Structure

### Per Bounded Context

```
com.k12.<bounded-context>/
│
├── domain/
│   ├── model/
│   │   ├── <Entity>.java              # Aggregate root (record)
│   │   ├── <ValueObject>.java         # Value objects (records)
│   │   ├── <Entity>Factory.java       # Create new entities
│   │   └── <Entity>Reconstructor.java # Apply events to entity
│   │
│   ├── commands/
│   │   └── <Entity>Commands.java      # Sealed interface of commands
│   │
│   ├── events/
│   │   └── <Entity>Events.java        # Sealed interface of events
│   │
│   ├── error/
│   │   └── <Entity>Error.java         # Sealed interface of domain errors
│   │
│   └── port/
│       ├── input/
│       │   └── <Entity>Repository.java    # Port: "I need to load/save entity"
│       │
│       └── output/
│           ├── EventStore.java            # Port: "I persist events"
│           ├── ProjectionUpdater.java     # Port: "I update projections"
│           └── SideEffectTrigger.java     # Port: "I trigger side effects"
│
├── application/
│   ├── input/
│   │   ├── <Operation>Input.java         # DTO → Command, validation
│   │   └── precondition/
│   │       └── <Condition>Checker.java   # Pre-condition checks
│   │
│   ├── service/
│   │   ├── CommandHandler.java           # Generic command handler
│   │   ├── Pipeline.java                 # Functional pipeline builder
│   │   └── saga/
│   │       └── <Saga>Orchestrator.java   # Multi-aggregate workflows
│   │
│   └── output/
│       ├── <Operation>Output.java        # Coordinates output operations
│       └── mapper/
│           └── ResponseMapper.java       # Events → ResponseDTO
│
└── infrastructure/
    ├── driving/
    │   ├── rest/
    │   │   ├── <Resource>Controller.java    # REST endpoints
    │   │   ├── <Resource>RequestDTO.java    # Request DTOs
    │   │   └── <Resource>ResponseDTO.java   # Response DTOs
    │   │
    │   ├── messaging/
    │   │   ├── <Message>Consumer.java       # Message queue consumers
    │   │   └── <Message>Producer.java       # Message queue producers
    │   │
    │   └── scheduler/
    │       └── <Job>Scheduler.java          # Scheduled jobs
    │
    └── driven/
        ├── persistence/
        │   ├── eventstore/
        │   │   └── PostgresEventStore.java  # EventStore implementation
        │   ├── projection/
        │   │   └── <Projection>Repository.java
        │   └── <Entity>RepositoryImpl.java  # Repository port impl
        │
        ├── messaging/
        │   └── kafka/
        │       └── KafkaEventPublisher.java
        │
        └── external/
            └── email/
                └── EmailService.java
```

### Shared Common Module

```
com.k12.common/
└── domain/
    └── model/
        ├── Result.java              # ROP Result type
        ├── UserId.java              # Common ID types
        ├── TenantId.java
        └── ...
```

---

## Request Processing Flow

### Complete Request Lifecycle

```
1. DRIVING ADAPTER
   REST Controller receives POST /users with CreateUserDTO

   ↓

2. INPUT LAYER (CreateUserInput)
   - Validate DTO format (@Valid, email format, etc.)
   - Convert DTO → CreateUserCommand
   - Return Result<Command, ValidationError>

   ↓

3. PRE-CONDITION CHECKS
   - Check if email already exists
   - Check if tenant is active
   - Return Result<Void, PreConditionError>

   ↓

4. APPLICATION SERVICE (CreateUserUserService)
   Uses CommandHandler:
   - loader: () → userRepository.findById(userId)
            → Result<User, RepositoryError>
            → returns User.initialState() for new user
   - processor: user → user.process(command)
            → Result<UserCreatedEvent, UserError>
   - mapper: event → UserResponseDTO.from(event)

   ↓

5. OUTPUT LAYER
   - eventStore.append(event) → Persist to events table
   - projectionUpdater.update(event) → Update read model
   - sideEffectTrigger.trigger(event) → Send welcome email (async)
   - Return Result<PersistedEvent, PersistenceError>

   ↓

6. DRIVEN ADAPTERS
   - EventStore writes to PostgreSQL events table
   - ProjectionUpdater writes to user_projections table
   - SideEffectTrigger publishes to async executor

   ↓

7. RESPONSE
   Controller returns:
   - 201 Created with UserResponseDTO
   - OR 400 with ValidationError
   - OR 409 with ConflictError
```

### Transaction Boundaries

- **Default**: Per-request transaction (simple operations)
- **Saga**: Multi-aggregate operations (each step = separate transaction)
- **Projections**: Synchronous (same transaction as event persistence)
- **Side Effects**: Asynchronous (separate executor, fire-and-forget)

---

## Error Handling

### Error Taxonomy

All errors are domain-specific with codes and metadata. No exceptions for business logic.

```
<Domain>Error (sealed interface)
├── ValidationError (HTTP 400)
│   - INVALID_EMAIL_FORMAT
│   - MISSING_REQUIRED_FIELD
│   - PASSWORD_TOO_WEAK
│
├── ConflictError (HTTP 409)
│   - EMAIL_ALREADY_IN_USE
│   - VERSION_CONFLICT
│
├── PreConditionError (HTTP 422)
│   - USER_NOT_FOUND
│   - USER_ALREADY_SUSPENDED
│
├── DomainError (HTTP 422)
│   - CANNOT_REMOVE_LAST_ROLE
│   - ROLE_ALREADY_ASSIGNED
│
└── PersistenceError (HTTP 500)
    - STORAGE_ERROR
```

### Result Type

```java
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    static <T, E> Result<T, E> success(T value);
    static <T, E> Result<T, E> failure(E error);

    boolean isSuccess();
    boolean isFailure();

    <U> Result<U, E> map(Function<T, U> mapper);
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);

    Result<T, E> peek(Consumer<T> consumer);
    Result<T, E> peekError(Consumer<E> consumer);

    T get();
    E getError();
}
```

### Error to HTTP Mapping

| Error Type | HTTP Status | Example |
|------------|-------------|---------|
| ValidationError | 400 | Invalid email format |
| ConflictError | 409 | Email already in use |
| PreConditionError | 422 | User not found |
| DomainError | 422 | Cannot remove last role |
| ConcurrencyError | 409 | Version conflict (retry) |
| PersistenceError | 500 | Database error |

---

## Testing Strategy

### Testing Pyramid

- **80% Unit Tests**: Domain layer, application logic
- **15% Integration Tests**: Adapters, ports
- **5% E2E Tests**: Critical user journeys

### Test Structure by Layer

```
src/test/java/com/k12/<bounded-context>/
│
├── domain/
│   ├── model/
│   │   └── <Entity>Test.java              # Entity behavior tests
│   └── port/
│       └── <Entity>RepositoryTest.java    # Port contract tests
│
├── application/
│   ├── input/
│   │   └── <Operation>InputTest.java
│   ├── service/
│   │   ├── CommandHandlerTest.java
│   │   └── <Operation>ServiceTest.java
│   └── output/
│       └── <Operation>OutputTest.java
│
└── infrastructure/
    ├── driving/
    │   └── rest/
    │       └── <Resource>ControllerTest.java
    └── driven/
        └── persistence/
            └── PostgresEventStoreTest.java
```

### Architecture Tests (ArchUnit)

Enforce dependency rules:

```java
@ArchTest
static final ArchRule domain_layer_should_not_depend_on_application =
    classes()
        .that().resideInAPackage("..domain..")
        .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "java..", "com.k12.common.domain..");
```

---

## Cross-Cutting Concerns

### Logging

- Use structured logging with SLF4J
- Log command execution and event generation
- Log errors with error codes and metadata

### Metrics

- Track command success/failure rates
- Measure command duration
- Tag metrics by bounded context and error type

### Tracing

- Use OpenTelemetry for distributed tracing
- Create spans for each saga step
- Record exceptions in traces on failure

### Saga Pattern

For multi-aggregate operations:

```java
// Example: Enroll Student in Course
1. Load Student (read-only, no compensation)
2. Load Course (read-only, no compensation)
3. Create Enrollment → compensate by deleting
4. Update Course Count → compensate by decrementing
```

Each step executes in its own transaction. On failure, previous steps are compensated in reverse order.

---

## Adoption Guidelines

### For Each New Bounded Context

1. **Copy package structure** from template
2. **Create domain model**:
   - Define entity as record
   - Create commands (sealed interface)
   - Create events (sealed interface)
   - Create errors (sealed interface with codes)
3. **Define ports**:
   - Repository interface (input port)
   - EventStore interface (output port)
4. **Implement application services**:
   - Use CommandHandler for simple flows
   - Use Pipeline for complex flows
   - Use Saga for multi-aggregate operations
5. **Implement adapters**:
   - REST controllers
   - EventStore implementation
   - Projection repositories

### Required Dependencies

```kotlin
// Quarkus
implementation("io.quarkus:quarkus-arc")
implementation("io.quarkus:quarkus-resteasy")
implementation("io.quarkus:quarkus-hibernate-validator")
implementation("io.quarkus:quarkus-jdbc-postgresql")

// jOOQ for database access
implementation("org.jooq:jooq:3.20.11")

// Functional programming
// Use Java 21+ sealed interfaces and pattern matching

// Testing
testImplementation("org.junit.jupiter:junit-jupiter")
testImplementation("org.assertj:assertj-core")
testImplementation("org.mockito:mockito-core")
testImplementation("com.tngtech.archunit:archunit")
```

### Code Quality Tools

- **Spotless**: Code formatting (Palantir Java Format)
- **ArchUnit**: Architecture compliance tests
- **Checkstyle**: Additional style checks (optional)

---

## Appendix: Code Examples

### Entity Example

```java
public record User(
    UserId userId,
    EmailAddress emailAddress,
    PasswordHash passwordHash,
    Set<UserRole> roles,
    UserStatus status,
    UserName name
) {
    public Result<UserEvents, UserError> process(UserCommands command) {
        return switch (command) {
            case SuspendUser c -> process(c);
            case ActivateUser c -> process(c);
            // ... other commands
        };
    }

    private Result<UserEvents, UserError> process(SuspendUser command) {
        if (this.status == UserStatus.SUSPENDED) {
            return Result.failure(USER_ALREADY_SUSPENDED);
        }
        return Result.success(new UserSuspended(userId, now(), version));
    }

    public User apply(UserEvents event) {
        return UserReconstructor.applyEvent(this, event);
    }
}
```

### CommandHandler Example

```java
@ApplicationScoped
public class CommandHandler<T, E> {
    private final EventStore eventStore;
    private final ProjectionUpdater projectionUpdater;
    private final SideEffectTrigger sideEffectTrigger;

    @Transactional
    public <R> Result<R, E> handle(
            Supplier<Result<T, E>> loader,
            Function<T, Result<Events, E>> processor,
            Function<Events, R> mapper) {

        return loader.get()
            .flatMap(processor)
            .flatMap(events ->
                eventStore.append(events)
                    .flatMap(persisted -> {
                        projectionUpdater.update(persisted);
                        sideEffectTrigger.triggerAsync(persisted);
                        return Result.success(mapper.apply(persisted));
                    })
            );
    }
}
```

---

**Document Version:** 1.0
**Last Updated:** 2026-02-22
**Maintained By:** Architecture Team
