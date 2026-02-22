# Quick Reference

> **Note:** This guide uses the **User** bounded context as a concrete example to illustrate patterns.
> The concepts, principles, and code patterns apply universally to any bounded context.
> Replace `User` with your entity (e.g., `Product`, `Order`, `Course`, `Invoice`) and
> `UserId` with your ID type — the architecture remains the same.

Cheat sheets and quick lookups for daily development.

## Result Type Combinators

```java
// Create
Result.success(value)
Result.failure(error)

// Check
result.isSuccess()
result.isFailure()

// Transform
result.map(fn)           // Transform success value
result.flatMap(fn)       // Chain Results
result.peek(consumer)    // Side effect on success
result.peekError(consumer)  // Side effect on failure

// Combine
result1.combine(result2, combiner)  // Combine two Results

// Get
result.get()             // Throw if failure
result.getOrDefault(default)  // Return default if failure
result.fold(onSuccess, onFailure)  // Convert to single value
```

## Common Result Patterns

### Validate multiple fields

```java
return EmailAddress.of(email)
    .combine(
        UserName.of(name),
        (email, name) -> new ValidatedUser(email, name)
    )
    .combine(
        PasswordHash.validate(password),
        (user, hash) -> user.withPassword(hash)
    );
```

### Chain operations

```java
return validate(request)
    .flatMap(this::checkPreConditions)
    .flatMap(this::persist)
    .peek(this::sendNotification);
```

### Handle errors

```java
result.fold(
    success -> Response.ok(success).build(),
    error -> ErrorResponseMapper.toResponse(error)
);
```

## Entity Template

```java
public record <EntityName>(
    <IdType> <idName>,
    <ValueType1> <field1>,
    <ValueType2> <field2>,
    ...
) {
    public Result<<Events>, <Error>> process(<Commands> command) {
        return switch (command) {
            case <Command1> c -> process(c);
            case <Command2> c -> process(c);
        };
    }

    private Result<<Events>, <Error>> process(<Command1> command) {
        // Business rules
        if (<invalid state>) {
            return Result.failure(<ERROR_CONSTANT>);
        }
        return Result.success(new <Event1>(...));
    }

    public <EntityName> apply(<Events> event) {
        return switch (event) {
            case <Event1> e -> new <EntityName>(..., e.<field>(), ...);
            case <Event2> e -> new <EntityName>(..., e.<field>(), ...);
        };
    }
}
```

## Value Object Template

```java
public record <ValueName>(String value) {
    private static final Pattern PATTERN =
        Pattern.compile("<regex>");

    public static Result<<ValueName>, String> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("<Error message>");
        }
        if (!PATTERN.matcher(value).matches()) {
            return Result.failure("<Error message>");
        }
        return Result.success(new <ValueName>(value));
    }
}
```

## Commands Template

```java
public sealed interface <Entity>Commands
        permits <Entity>Commands.<Command1>,
                <Entity>Commands.<Command2> {

    record <Command1>(<Type1> <field1>, <Type2> <field2>)
        implements <Entity>Commands {}

    record <Command2>(<Type> <field>)
        implements <Entity>Commands {}
}
```

## Events Template

```java
public sealed interface <Entity>Events
        permits <Entity>Events.<Event1>,
                <Entity>Events.<Event2> {

    record <Event1>(
        <IdType> <idName>,
        <Fields...>,
        Instant timestamp,
        long version
    ) implements <Entity>Events {}

    record <Event2>(
        <IdType> <idName>,
        <Fields...>,
        Instant timestamp,
        long version
    ) implements <Entity>Events {}
}
```

## Errors Template

```java
public sealed interface <Entity>Error permits
        <Entity>Error.ValidationError,
        <Entity>Error.ConflictError,
        <Entity>Error.PreConditionError,
        <Entity>Error.DomainError {

    String code();
    String message();
    Map<String, Object> metadata();

    record ValidationError(
        String code,
        String message,
        Map<String, Object> metadata
    ) implements <Entity>Error {

        public static final ValidationError <ERROR_NAME> =
            new ValidationError("<CODE>", "<Message>", Map.of(...));
    }

    // ... other error types
}
```

## Port Templates

### Input Port (Repository)

```java
public interface <Entity>Repository {
    Result<<Entity>, <Entity>Error> findById(<IdType> id);
    boolean exists(<ValueObject> value);
}
```

### Output Port (EventStore)

```java
public interface EventStore {
    Result<PersistedEvent, <Entity>Error> append(<Events> event);
    Result<List<<Events>>, <Entity>Error> load(String entityId);

    record PersistedEvent(
        <Events> event,
        long version,
        Instant persistedAt
    ) {}
}
```

## CommandHandler Usage

```java
@ApplicationScoped
public class <Operation>Service {
    private final CommandHandler<<Entity>, <Entity>Error> handler;
    private final <Entity>Repository repository;

    public Result<ResponseDTO, <Entity>Error> execute(RequestDTO request) {
        return handler.handle(
            // Loader
            () -> repository.findById(request.id()),

            // Processor
            entity -> entity.process(toCommand(request)),

            // Mapper
            persisted -> ResponseDTO.from(persisted)
        );
    }
}
```

## Validation Layers

### Layer 1: DTO Validation (Input)

```java
// In REST controller
@POST
public Response create(@Valid @BeanParam CreateUserRequest request) {
    // @Valid handles format, required fields
}
```

### Layer 2: Pre-condition Checks

```java
// Before loading entity
var emailExists = userRepository.emailExists(email);
if (emailExists) {
    return Result.failure(EMAIL_ALREADY_IN_USE);
}
```

### Layer 3: Domain Validation

```java
// In entity.process()
if (status == SUSPENDED) {
    return Result.failure(USER_ALREADY_SUSPENDED);
}
```

## HTTP Status Mapping

| Error Type | Status | Example |
|------------|--------|---------|
| ValidationError | 400 | INVALID_EMAIL_FORMAT |
| ConflictError | 409 | EMAIL_ALREADY_IN_USE |
| PreConditionError | 422 | USER_ALREADY_SUSPENDED |
| DomainError | 422 | CANNOT_REMOVE_LAST_ROLE |
| ConcurrencyError | 409 | VERSION_CONFLICT |
| PersistenceError | 500 | STORAGE_ERROR |

## Testing Patterns

### Domain Test

```java
@Test
@DisplayName("<description>")
void <testName>() {
    // Given
    var entity = new <Entity>(...);
    var command = new <Command>(...);

    // When
    var result = entity.process(command);

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.get()).isInstanceOf(<Event>.class);
}
```

### Service Test

```java
@ExtendWith(MockitoExtension.class)
class <Service>Test {
    @Mock <Entity>Repository repository;
    @Mock EventStore eventStore;

    @Test
    void <testName>() {
        // Given
        when(repository.findById(id))
            .thenReturn(Result.success(entity));

        // When
        var result = service.execute(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(eventStore).append(event);
    }
}
```

## Package Structure Quick Reference

```
com.k12.<bounded-context>/
├── domain/
│   ├── model/
│   │   ├── <Entity>.java
│   │   ├── <ValueObject>.java
│   │   ├── <Entity>Factory.java
│   │   └── <Entity>Reconstructor.java
│   ├── commands/
│   │   └── <Entity>Commands.java
│   ├── events/
│   │   └── <Entity>Events.java
│   ├── error/
│   │   └── <Entity>Error.java
│   └── port/
│       ├── input/
│       │   └── <Entity>Repository.java
│       └── output/
│           ├── EventStore.java
│           └── ProjectionUpdater.java
│
├── application/
│   ├── input/
│   │   └── <Operation>Input.java
│   ├── service/
│   │   └── <Operation>Service.java
│   └── output/
│       └── <Operation>Output.java
│
└── infrastructure/
    ├── driving/
    │   └── rest/
    │       └── <Resource>Controller.java
    └── driven/
        └── persistence/
            └── <Entity>RepositoryImpl.java
```

## ArchUnit Rules (Quick Copy)

```java
// Domain purity
classes().that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
        .resideInAnyPackage("..domain..", "java..", "com.k12.common.domain..");

// Entities are records
classes().that().haveSimpleName("User")
    .should().beRecords();

// Commands are sealed interfaces
classes().that().haveSimpleNameEndingWith("Commands")
    .should().beInterfaces()
    .andShould().beModifiers().withOnlySealedInterfaces();
```

## Common Git Commit Messages

```bash
# New feature
feat(<context>): add <feature>

# Bug fix
fix(<context>): <description>

# Documentation
docs(<section>): <description>

# Refactoring
refactor(<context>): <description>

# Test
test(<context>): add tests for <feature>

# Example
feat(user): add suspend user command
fix(user): handle null email in validation
docs(kb): add quick reference guide
```

## Gradle Commands

```bash
# Build
./gradlew build

# Test
./gradlew test
./gradlew test --tests <TestClassName>

# Format
./gradlew spotlessApply
./gradlew spotlessCheck

# Architecture tests
./gradlew test --tests "*ArchitectureTest"

# Run application
./gradlew quarkusDev
```

## IDE Shortcuts (IntelliJ IDEA)

| Action | Shortcut |
|--------|----------|
| Generate record | Cmd+N → Record |
| Create sealed interface | Cmd+N → Interface |
| Show type hints | Cmd+Shift+P |
| Run test | Ctrl+R |
| Debug test | Ctrl+D |
| Format code | Cmd+Alt+L |

---

**Need more detail?**
- See [Common Tasks](./17-Common-Tasks.md) for step-by-step procedures
- See [Troubleshooting](./18-Troubleshooting.md) for issues
- See [FAQ](./19-FAQ.md) for questions
