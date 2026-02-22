# Getting Started Guide

This guide will help you understand and start using the Domain-Centric Architecture for Java backend applications.

## What is Domain-Centric Architecture?

Domain-Centric Architecture is a software design approach that places business logic at the center of your application. The domain layer is pure, containing no dependencies on external concerns like databases, web frameworks, or messaging systems.

### Key Characteristics

- **Business logic isolation**: Domain rules live independently of technical implementation
- **Type safety**: Sealed interfaces prevent invalid states
- **Testability**: Domain logic can be tested without external dependencies
- **Flexibility**: Swap infrastructure implementations without touching business logic

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Driving Adapters                     │
│  (REST Controllers, Message Consumers, Schedulers)      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    Input Layer                           │
│  DTO validation → Command conversion                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                 Application Services                     │
│  CommandHandler → orchestrates the flow                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    Domain Core                           │
│  Entities process Commands → return Events              │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    Driven Adapters                       │
│  EventStore, Projections, External Services             │
└─────────────────────────────────────────────────────────┘
```

## Core Concepts

### 1. Railway Oriented Programming (ROP)

Instead of exceptions, we use the `Result<T, E>` type:

```java
// Instead of throwing exceptions
public User findUser(String id) throws UserNotFoundException {
    if (notFound) throw new UserNotFoundException();
    return user;
}

// Use Result type
public Result<User, UserError> findUser(String id) {
    if (notFound) return Result.failure(USER_NOT_FOUND);
    return Result.success(user);
}
```

**Benefits:**
- Errors are explicit in type signatures
- Compiler forces error handling
- Easy to chain operations without try-catch

### 2. Commands and Events

**Command**: Intent to do something
```java
sealed interface UserCommands {
    record SuspendUser(UserId userId) implements UserCommands {}
    record ActivateUser(UserId userId) implements UserCommands {}
}
```

**Event**: Something that happened
```java
sealed interface UserEvents {
    record UserSuspended(UserId userId, Instant when, long version) implements UserEvents {}
    record UserActivated(UserId userId, Instant when, long version) implements UserEvents {}
}
```

### 3. Entity Behavior

Entities process commands and return events:

```java
public record User(...) {
    public Result<UserEvents, UserError> process(UserCommands command) {
        return switch (command) {
            case SuspendUser c -> process(c);
            case ActivateUser c -> process(c);
        };
    }

    private Result<UserEvents, UserError> process(SuspendUser c) {
        if (status == SUSPENDED) return Result.failure(USER_ALREADY_SUSPENDED);
        return Result.success(new UserSuspended(userId, now(), version));
    }
}
```

### 4. Ports and Adapters

**Ports** (in domain): Interfaces defining what the domain needs
```java
// Domain port
interface UserRepository {
    Result<User, UserError> findById(UserId id);
}
```

**Adapters** (in infrastructure): Implementations of ports
```java
// Infrastructure adapter
class PostgresUserRepository implements UserRepository {
    public Result<User, UserError> findById(UserId id) {
        // JDBC/jOOQ implementation
    }
}
```

## Request Flow Example

Let's trace a "Suspend User" request:

```
1. REST Controller
   → Receives POST /users/{id}/suspend

2. Input Layer
   → Validates userId format
   → Creates SuspendUser(userId) command

3. Application Service (CommandHandler)
   → Loads User via UserRepository port
   → Calls user.process(command)

4. Domain Entity (User)
   → Checks if already suspended
   → Returns UserSuspended event OR error

5. Output Layer
   → EventStore persists event
   → ProjectionUpdater updates read model
   → SideEffectTrigger sends notification (async)

6. Response
   → 200 OK with success message
   → OR 422/409 with error details
```

## Project Structure

Each bounded context follows this structure:

```
com.k12.<bounded-context>/
├── domain/                    # Pure business logic
│   ├── model/                # Entities, value objects
│   ├── commands/             # Command definitions
│   ├── events/               # Event definitions
│   ├── error/                # Error definitions
│   └── port/                 # Port interfaces
│
├── application/              # Orchestration
│   ├── input/                # DTO → Command
│   ├── service/              # CommandHandler, Pipeline
│   └── output/               # Event coordination
│
└── infrastructure/           # Technical details
    ├── driving/              # REST, messaging
    └── driven/               # Database, external services
```

## Your First Task

Choose based on your role:

### New Developer
1. Read [Domain Model Guide](./04-Domain-Model-Guide.md)
2. Study the User bounded context example
3. Try [Common Tasks](./17-Common-Tasks.md)

### Starting New Project
1. Follow [Project Setup](./02-Project-Setup.md)
2. Create your first bounded context using [Creating Bounded Context](./03-Creating-Bounded-Context.md)

### Migrating Existing Code
1. Read [Migration Guide](./20-Migration-Guide.md)
2. Start with a non-critical bounded context
3. Reference [Common Tasks](./17-Common-Tasks.md) for patterns

## Common Patterns Reference

| Task | Guide |
|------|-------|
| Create new entity | [Domain Model Guide](./04-Domain-Model-Guide.md) |
| Add new command | [Commands Events Guide](./05-Commands-Events-Guide.md) |
| Handle validation errors | [Error Handling Guide](./06-Error-Handling-Guide.md) |
| Write tests | [Testing Strategies](./10-Testing-Strategies.md) |
| Add REST endpoint | [Common Tasks](./17-Common-Tasks.md#rest-endpoints) |

## Next Steps

1. ✅ Read this Getting Started Guide
2. ➡️ Continue to [Project Setup](./02-Project-Setup.md) OR
3. ➡️ Jump to [Creating Bounded Context](./03-Creating-Bounded-Context.md)

## Questions?

See the [FAQ](./19-FAQ.md) for common questions or check [Troubleshooting](./18-Troubleshooting.md) for issues.

---

**Related:**
- [Architecture Overview](../architecture-overview.md)
- [Quick Reference](./16-Quick-Reference.md)
