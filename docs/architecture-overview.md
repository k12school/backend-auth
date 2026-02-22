# Domain-Centric Architecture Overview

This document provides a high-level overview of the Domain-Centric Architecture used in this project.

## What is Domain-Centric Architecture?

Domain-Centric Architecture is a software design approach that places **business logic at the center** of your application. The domain layer is pure and independent, containing no dependencies on external concerns like databases, web frameworks, or messaging systems.

## Key Principles

1. **Domain Centric** - All business logic lives in the domain layer
2. **Ports & Adapters** - Domain defines ports, infrastructure implements adapters
3. **ROP Only** - Railway Oriented Programming, no exceptions for business logic
4. **Event Sourcing** - Entities are projections of their event stream
5. **Immutable** - All entities and value objects are immutable records
6. **Sealed Types** - Type-safe commands, events, and errors

## Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Driving Adapters                     │
│  REST Controllers | Message Consumers | Schedulers     │
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
│  CommandHandler orchestrates the flow                   │
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
│  EventStore | Projections | External Services           │
└─────────────────────────────────────────────────────────┘
```

## Request Flow Example

```
1. REST Controller receives POST /users/{id}/suspend
   ↓
2. Input Layer validates DTO, creates SuspendUser command
   ↓
3. Application Service loads User via Repository
   ↓
4. User entity processes command
   - Returns UserSuspended event OR error
   ↓
5. EventStore persists event
   ↓
6. ProjectionUpdater updates read model
   ↓
7. SideEffectTrigger sends notification (async)
   ↓
8. Response returned (200 OK or error)
```

## Error Handling

All errors are domain-specific with codes:

| Error Type | HTTP Status | Example |
|------------|-------------|---------|
| ValidationError | 400 | INVALID_EMAIL_FORMAT |
| ConflictError | 409 | EMAIL_ALREADY_IN_USE |
| PreConditionError | 422 | USER_ALREADY_SUSPENDED |
| DomainError | 422 | CANNOT_REMOVE_LAST_ROLE |
| ConcurrencyError | 409 | VERSION_CONFLICT |
| PersistenceError | 500 | STORAGE_ERROR |

Instead of exceptions, we use the `Result<T, E>` type for explicit error handling.

## Package Structure

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

## Documentation

For comprehensive guides and references, see the **Knowledge Base**:

- [Knowledge Base Landing](./knowledge-base/00-landing-page.md) - Full documentation index
- [Getting Started Guide](./knowledge-base/01-Getting-Started-Guide.md) - Start here
- [Quick Reference](./knowledge-base/16-Quick-Reference.md) - Daily cheat sheets
- [Common Tasks](./knowledge-base/17-Common-Tasks.md) - Step-by-step procedures
- [FAQ](./knowledge-base/19-FAQ.md) - Frequently asked questions

## Further Reading

- [Architecture Design Specification](./plans/2026-02-22-domain-centric-architecture-design.md) - Complete architecture specification
- [Implementation Plan](./plans/2026-02-22-domain-centric-architecture-implementation.md) - Step-by-step implementation guide

---

**Last Updated:** 2026-02-22
**Maintained By:** Architecture Team
