# Domain-Centric Architecture Knowledge Base

Welcome to the Domain-Centric Architecture knowledge base. This resource provides comprehensive guidance for implementing company-wide Java backend applications using our standardized architecture.

## 📚 Documentation Index

### Getting Started
- [01-Getting-Started-Guide.md](./01-Getting-Started-Guide.md) - Begin here for new teams/projects
- [02-Project-Setup.md](./02-Project-Setup.md) - Initial project configuration
- [03-Creating-Bounded-Context.md](./03-Creating-Bounded-Context.md) - Setting up a new bounded context

### Core Concepts
- [04-Domain-Model-Guide.md](./04-Domain-Model-Guide.md) - Entities, value objects, aggregates
- [05-Commands-Events-Guide.md](./05-Commands-Events-Guide.md) - Understanding commands and events
- [06-Error-Handling-Guide.md](./06-Error-Handling-Guide.md) - ROP and error taxonomy
- [07-Ports-Adapters-Guide.md](./07-Ports-Adapters-Guide.md) - Hexagonal architecture

### Implementation Patterns
- [08-Application-Services-Guide.md](./08-Application-Services-Guide.md) - CommandHandler, Pipeline, Saga
- [09-Validation-Patterns.md](./09-Validation-Patterns.md) - Three-layer validation
- [10-Testing-Strategies.md](./10-Testing-Strategies.md) - Unit, integration, E2E tests
- [11-Event-Sourcing-Patterns.md](./11-Event-Sourcing-Patterns.md) - Event store and projections

### Best Practices
- [12-Coding-Standards.md](./12-Coding-Standards.md) - Code style and conventions
- [13-Architecture-Rules.md](./13-Architecture-Rules.md) - ArchUnit rules and compliance
- [14-Performance-Guidelines.md](./14-Performance-Guidelines.md) - Optimization patterns
- [15-Security-Guidelines.md](./15-Security-Guidelines.md) - Security best practices

### Reference
- [16-Quick-Reference.md](./16-Quick-Reference.md) - Cheat sheets and quick lookups
- [17-Common-Tasks.md](./17-Common-Tasks.md) - Step-by-step common operations
- [18-Troubleshooting.md](./18-Troubleshooting.md) - Common issues and solutions
- [19-FAQ.md](./19-FAQ.md) - Frequently asked questions

### Migration
- [20-Migration-Guide.md](./20-Migration-Guide.md) - Migrating existing codebases

## 🎯 Architecture Principles

Our architecture is built on these core principles:

1. **Domain Centric** - All business logic lives in the domain layer
2. **Ports & Adapters** - Domain defines ports, infrastructure implements adapters
3. **ROP Only** - Railway Oriented Programming, no exceptions for business logic
4. **Event Sourcing** - Entities are projections of their event stream
5. **Immutable** - All entities and value objects are immutable records
6. **Sealed Types** - Type-safe commands, events, and errors

## 🚀 Quick Start

1. **Read** the [Getting Started Guide](./01-Getting-Started-Guide.md)
2. **Follow** the [Project Setup](./02-Project-Setup.md) instructions
3. **Create** your first bounded context using the [Creating Bounded Context](./03-Creating-Bounded-Context.md) guide
4. **Reference** the [Quick Reference](./16-Quick-Reference.md) for daily work

## 📖 Learning Path

### For New Developers
1. Start with [Getting Started Guide](./01-Getting-Started-Guide.md)
2. Read [Domain Model Guide](./04-Domain-Model-Guide.md)
3. Understand [Error Handling](./06-Error-Handling-Guide.md)
4. Learn [Testing Strategies](./10-Testing-Strategies.md)

### For Architects
1. Review [Architecture Rules](./13-Architecture-Rules.md)
2. Study [Ports & Adapters Guide](./07-Ports-Adapters-Guide.md)
3. Reference [Performance Guidelines](./14-Performance-Guidelines.md)

### For Migration Teams
1. Read [Migration Guide](./20-Migration-Guide.md)
2. Follow [Common Tasks](./17-Common-Tasks.md) for patterns
3. Check [Troubleshooting](./18-Troubleshooting.md) for issues

## 🔗 Related Documents

- [Architecture Design Specification](../plans/2026-02-22-domain-centric-architecture-design.md)
- [Implementation Plan](../plans/2026-02-22-domain-centric-architecture-implementation.md)
- [Architecture Overview](../architecture-overview.md)

## 💡 Contributing

To improve this knowledge base:
1. Update existing documents with lessons learned
2. Add new patterns discovered during implementation
3. Submit examples from your bounded contexts
4. Update FAQ with common questions

---

**Last Updated:** 2026-02-22
**Maintained By:** Architecture Team
