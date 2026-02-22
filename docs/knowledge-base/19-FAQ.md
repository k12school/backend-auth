# Frequently Asked Questions

Common questions about the Domain-Centric Architecture.

## General Questions

### Q: Why not use exceptions for errors?

**A:** Exceptions are for exceptional circumstances (system failures), not business logic. Using `Result<T, E>` makes errors:
- **Explicit** - Visible in method signatures
- **Handled** - Compiler forces you to deal with them
- **Testable** - Easy to assert on error conditions
- **Composable** - Can chain operations without try-catch

### Q: Isn't this architecture verbose?

**A:** It requires more code upfront, but pays off in:
- **Maintainability** - Clear separation of concerns
- **Testability** - Easy to test domain logic in isolation
- **Flexibility** - Swap implementations without touching business logic
- **Onboarding** - New developers can understand the structure quickly

The "verbosity" is explicit business logic that would otherwise be hidden.

### Q: Can I use Spring instead of Quarkus?

**A:** Yes! The architecture is framework-agnostic. The core principles apply regardless of framework. Adapt the infrastructure layer to your chosen framework.

### Q: Do I need to use Event Sourcing?

**A:** Event Sourcing is the default pattern, but you can adapt:
- **Event Sourcing** - Recommended for audit trails, complex workflows
- **Hybrid** - Store events + current state in separate tables
- **Traditional** - Only store current state, events are transient

## Domain Layer

### Q: How do I handle database transactions in the domain?

**A:** You don't! The domain layer has no concept of databases. Transactions are managed in the **application layer** (CommandHandler) or **infrastructure layer** (repository implementations).

### Q: Can entities call services/repositories?

**A:** No. Entities must remain pure. All external dependencies go through ports/interfaces, and those are used by the application layer, not the domain.

### Q: How do I share logic between entities?

**A:** Three options:

1. **Value objects** - Put shared logic in reusable VOs
2. **Domain services** - Create stateless services in domain layer
3. **Factory/Reconstructor** - Put creation/reconstruction logic there

```java
// Option 1: Value object with behavior
public record EmailAddress(String value) {
    public boolean isCorporate() {
        return value.endsWith("@company.com");
    }
}

// Option 2: Domain service
public final class EmailDomainService {
    public static boolean canSendTo(EmailAddress from, EmailAddress to) {
        // Business rules that involve multiple entities
    }
}
```

### Q: What if my command needs to return multiple events?

**A:** Use a list or create a specific "Events" container:

```java
// Option 1: Return list
public Result<List<UserEvents>, UserError> process(Command c);

// Option 2: Use a container
public sealed interface UserEvents {
    record Multiple(List<UserEvents> events) implements UserEvents {}
}
```

### Q: How do I handle version conflicts?

**A:** The EventStore handles optimistic locking:

```java
// In EventStore implementation
try {
    jooq.insertInto(EVENTS)
        .set(VERSION, event.version())
        .set(EVENT_DATA, serialize(event))
        .execute();
} catch (DataIntegrityViolationException e) {
    return Result.failure(VERSION_CONFLICT);
}
```

## Application Layer

### Q: When should I use CommandHandler vs Pipeline?

**A:** Use **CommandHandler** for simple flows (load → process → persist). Use **Pipeline** for:
- Multiple sequential steps
- Complex error handling
- Custom flow control

### Q: How do I handle multi-entity operations?

**A:** Use a **Saga Orchestrator**:

```java
saga.execute(
    loadStudent,       // Step 1
    loadCourse,        // Step 2
    createEnrollment,  // Step 3 (with compensation)
    updateCapacity     // Step 4 (with compensation)
);
```

### Q: Should I validate DTOs in the controller or input layer?

**A:** Two layers:

1. **Controller** - Use `@Valid` for format validation (Bean Validation)
2. **Input layer** - Convert to commands, check business pre-conditions

## Infrastructure Layer

### Q: How do I implement the EventStore?

**A:** Simplest approach - PostgreSQL table:

```sql
CREATE TABLE user_events (
    id SERIAL PRIMARY KEY,
    aggregate_id VARCHAR NOT NULL,
    event_type VARCHAR NOT NULL,
    event_data JSONB NOT NULL,
    version BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    UNIQUE(aggregate_id, version)
);
```

Use jOOQ or JDBC for reads/writes. For higher throughput, consider Kafka.

### Q: How do projections work?

**A:** Projections are read-optimized tables updated when events are persisted:

```java
// In ProjectionUpdater
public Result<Void, Error> update(PersistedEvent event) {
    return switch(event.event()) {
        case UserCreated e -> jooq.insertInto(USER_PROJECTION)
            .set(ID, e.userId().value())
            .set(EMAIL, e.email().value())
            .execute();
        case UserSuspended e -> jooq.update(USER_PROJECTION)
            .set(STATUS, "SUSPENDED")
            .where(ID.eq(e.userId().value()))
            .execute();
    };
}
```

### Q: How do I test external service calls?

**A:** Mock the port interface:

```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock EmailServicePort emailService;

    @Test
    void test() {
        when(emailService.send(any())).thenReturn(Result.success(null));
        // ... test code
    }
}
```

## Testing

### Q: Do I need to test every branch?

**A:** Yes, aim for 100% coverage on domain logic. Domain rules are critical and must be verified.

### Q: How do I test error conditions?

**A:** Use `assertThat(result.isFailure())` and assert on the error:

```java
@Test
void suspendAlreadySuspendedUserShouldReturnError() {
    var result = suspendedUser.process(suspendCommand);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError())
        .isEqualTo(USER_ALREADY_SUSPENDED);
}
```

### Q: Should I mock repositories in domain tests?

**A:** No. Domain tests should not use repositories at all. Test entities directly by calling `process()` with commands.

## Performance

### Q: Isn't Event Sourcing slow?

**A:** Event Sourcing adds overhead, but:
- **Projections** provide fast reads
- **Caching** reduces event replay
- **Snapshots** can store periodic state
- The tradeoff is often worth it for audit/debugging benefits

### Q: How do I handle hot aggregates?

**A:** Options:
- **Caching** - Cache current state in memory
- **Sharding** - Split by tenant/customer
- **Snapshots** - Store state every N events
- **Optimistic locking** - Minimizes lock duration

### Q: Should I batch event processing?

**A:** For high-volume scenarios, yes:
```java
eventStore.appendAll(events);  // Single transaction
projectionUpdater.updateAll(events);  // Batch update
```

## Migration

### Q: How do I migrate existing code?

**A:** Gradual approach:
1. **Start new bounded contexts** with this architecture
2. **Strangler fig pattern** - Gradually replace old code
3. **Anti-corruption layer** - Translate between old/new
4. **Event bridge** - Emit events from legacy system

See [Migration Guide](./20-Migration-Guide.md) for details.

### Q: Can I mix old and new code?

**A:** Yes, in different bounded contexts. Keep the interface clean between them using ports/adapters.

## Troubleshooting

### Q: My tests are failing with "class not found"

**A:** Check:
1. Are files in correct packages?
2. Did you run `./gradlew build`?
3. Are generated files (jOOQ) available?

### Q: ArchUnit tests fail with dependency violations

**A:** Common issues:
- Domain imports from infrastructure → Move to port interface
- Application imports concrete class → Depend on port, not implementation
- Test code in wrong package → Move to src/test

### Q: Events not persisting

**A:** Check:
1. `@Transactional` annotation present?
2. EventStore implementation correct?
3. Database connection valid?
4. Unique constraint violation (version conflict)?

## Best Practices

### Q: How large should a bounded context be?

**A:** Rule of thumb: **One domain = one bounded context**. If User and Order are in different domains with different teams, separate them. If they're tightly coupled, keep together.

### Q: Should I share code between bounded contexts?

**A:** Only **common domain concepts**:
- ID types (UserId, etc.)
- Result type
- Shared value objects

**Don't share**:
- Entities
- Business rules
- Infrastructure

### Q: How do I handle cross-cutting concerns?

**A:** Use **interceptors/decorators**:
- Logging → Around advice in CommandHandler
- Metrics → Wrap CommandHandler
- Security → In REST controller layer

---

**Still have questions?**
- Check [Troubleshooting](./18-Troubleshooting.md)
- Review [Getting Started Guide](./01-Getting-Started-Guide.md)
- Ask in team Slack/Teams channel
