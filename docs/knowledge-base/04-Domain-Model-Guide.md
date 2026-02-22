# Domain Model Guide

This guide explains how to design and implement domain models following our architecture principles.

## Core Concepts

### Entities

Entities are the heart of your domain. They are:
- **Immutable**: Records that cannot be modified after creation
- **Identifiable**: Have a unique ID
- **Behavioral**: Process commands and return events

#### Entity Example

```java
public record User(
    UserId userId,
    EmailAddress emailAddress,
    UserName name,
    Set<UserRole> roles,
    UserStatus status
) {
    public Result<UserEvents, UserError> process(UserCommands command) {
        return switch (command) {
            case SuspendUser c -> process(c);
            case ActivateUser c -> process(c);
            case UpdateName c -> process(c);
        };
    }

    private Result<UserEvents, UserError> process(SuspendUser c) {
        if (status == SUSPENDED) {
            return Result.failure(USER_ALREADY_SUSPENDED);
        }
        return Result.success(new UserSuspended(userId, now(), nextVersion()));
    }

    private Result<UserEvents, UserError> process(UpdateName c) {
        if (name.value().equals(c.newName().value())) {
            return Result.failure(NAME_SAME_AS_CURRENT);
        }
        return Result.success(new UserNameUpdated(userId, c.newName(), name.value(), now(), nextVersion()));
    }
}
```

#### Entity Guidelines

| Rule | Reason |
|------|--------|
| Use `record` | Immutability, equals/hashCode auto-generated |
| One aggregate root per file | Clear separation of concerns |
| Return `Result<Event, Error>` | Explicit error handling, no exceptions |
| Use pattern matching on commands | Type-safe, exhaustive handling |
| No dependency injection | Domain stays pure |
| No database annotations | Infrastructure concern only |

### Value Objects

Value objects represent domain concepts identified by their attributes, not by ID.

#### Value Object Example

```java
public record EmailAddress(String value) {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static Result<EmailAddress, String> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Email cannot be null or blank");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            return Result.failure("Invalid email format");
        }
        return Result.success(new EmailAddress(value));
    }
}
```

#### Value Object Guidelines

| Rule | Reason |
|------|--------|
| Static factory `of()` returning `Result` | Validation on construction |
| Private constructor | Forces use of factory |
| Immutable `record` | Cannot be modified after creation |
| Self-validating | Cannot exist in invalid state |
| No ID | Identified by value, not identity |

### Common Value Object Patterns

#### 1. String Wrapper with Validation

```java
public record UserName(String value) {
    public static Result<UserName, String> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Name cannot be null or blank");
        }
        if (value.length() < 2 || value.length() > 100) {
            return Result.failure("Name must be 2-100 characters");
        }
        return Result.success(new UserName(value));
    }
}
```

#### 2. Numeric Range

```java
public record Percentage(int value) {
    public static Result<Percentage, String> of(int value) {
        if (value < 0 || value > 100) {
            return Result.failure("Percentage must be 0-100");
        }
        return Result.success(new Percentage(value));
    }
}
```

#### 3. Enumeration Wrapper

```java
public record UserRole(String value) {
    public static final UserRole ADMIN = new UserRole("ADMIN");
    public static final UserRole TEACHER = new UserRole("TEACHER");
    public static final UserRole STUDENT = new UserRole("STUDENT");

    public static Result<UserRole, String> of(String value) {
        if (!Set.of("ADMIN", "TEACHER", "STUDENT").contains(value)) {
            return Result.failure("Invalid role: " + value);
        }
        return Result.success(new UserRole(value));
    }
}
```

### Factories

Factories create complex entities and their initial events.

```java
public final class UserFactory {

    public static Result<UserEvents.UserCreated, UserError> createNew(
            UserCommands.CreateUser command) {

        // Validate all components
        var emailResult = EmailAddress.of(command.email());
        var nameResult = UserName.of(command.name());

        if (emailResult.isFailure()) {
            return Result.failure(INVALID_EMAIL_FORMAT);
        }
        if (nameResult.isFailure()) {
            return Result.failure(INVALID_NAME_FORMAT);
        }

        // Create the initial event
        return Result.success(new UserEvents.UserCreated(
            UserId.generate(),
            emailResult.get(),
            PasswordHash.hash(command.password()),
            command.roles(),
            UserStatus.PENDING_ACTIVATION,
            nameResult.get(),
            Instant.now(),
            1L
        ));
    }
}
```

### Reconstructors

Reconstructors rebuild entity state from events.

```java
public final class UserReconstructor {

    public static User reconstruct(List<UserEvents> events) {
        User user = null;

        for (UserEvents event : events) {
            if (event instanceof UserCreated created) {
                user = new User(
                    created.userId(),
                    created.email(),
                    PasswordHash.of(created.passwordHash()),
                    created.roles(),
                    created.status(),
                    created.name()
                );
            } else if (user != null) {
                user = applyEvent(user, event);
            }
        }

        return user;
    }

    public static User applyEvent(User user, UserEvents event) {
        return switch (event) {
            case UserSuspended e ->
                new User(user.userId(), user.emailAddress(), user.name(),
                         user.roles(), UserStatus.SUSPENDED);
            case UserActivated e ->
                new User(user.userId(), user.emailAddress(), user.name(),
                         user.roles(), UserStatus.ACTIVE);
            case UserNameUpdated e ->
                new User(user.userId(), user.emailAddress(), e.newName(),
                         user.roles(), user.status());
            default -> user;
        };
    }
}
```

## Domain Logic Patterns

### 1. State Validation

```java
private Result<UserEvents, UserError> process(SuspendUser command) {
    // Check current state
    if (status == SUSPENDED) {
        return Result.failure(USER_ALREADY_SUSPENDED);
    }

    // Business rule: active users can be suspended
    if (status != ACTIVE) {
        return Result.failure(CANNOT_SUSPEND_NON_ACTIVE);
    }

    return Result.success(new UserSuspended(userId, now(), nextVersion()));
}
```

### 2. Collection Operations

```java
private Result<UserEvents, UserError> process(AddRole command) {
    // Check if role already exists
    if (roles.contains(command.role())) {
        return Result.failure(ROLE_ALREADY_ASSIGNED);
    }

    // Business rule: max 5 roles
    if (roles.size() >= 5) {
        return Result.failure(TOO_MANY_ROLES);
    }

    return Result.success(new UserRoleAdded(userId, command.role(), now(), nextVersion()));
}

private Result<UserEvents, UserError> process(RemoveRole command) {
    if (!roles.contains(command.role())) {
        return Result.failure(ROLE_NOT_FOUND);
    }

    // Business rule: must have at least one role
    if (roles.size() == 1) {
        return Result.failure(CANNOT_REMOVE_LAST_ROLE);
    }

    return Result.success(new UserRoleRemoved(userId, command.role(), now(), nextVersion()));
}
```

### 3. Value Comparison

```java
private Result<UserEvents, UserError> process(UpdateEmail command) {
    // Business rule: email must actually change
    if (emailAddress.value().equals(command.newEmail().value())) {
        return Result.failure(EMAIL_SAME_AS_CURRENT);
    }

    return Result.success(new UserEmailUpdated(
        userId, command.newEmail(), emailAddress.value(), now(), nextVersion()
    ));
}
```

### 4. Conditional Events

```java
private Result<UserEvents, UserError> process(ChangePassword command) {
    // Validate password strength
    var strengthResult = PasswordStrength.check(command.newPassword());
    if (strengthResult.isFailure()) {
        return Result.failure(PASSWORD_TOO_WEAK);
    }

    // Only generate event if password actually changed
    if (passwordHash.equals(command.newPasswordHash())) {
        return Result.failure(PASSWORD_SAME_AS_CURRENT);
    }

    return Result.success(new UserPasswordChanged(userId, now(), nextVersion()));
}
```

## Testing Domain Models

### Unit Test Example

```java
class UserTest {

    @Test
    @DisplayName("Suspend active user should return UserSuspended event")
    void suspendActiveUserShouldReturnEvent() {
        // Given
        var user = new User(
            userId, email, name, Set.of(STUDENT), ACTIVE
        );
        var command = new SuspendUser(userId);

        // When
        var result = user.process(command);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get())
            .isInstanceOf(UserSuspended.class);
    }

    @Test
    @DisplayName("Suspend already suspended user should return error")
    void suspendSuspendedUserShouldReturnError() {
        // Given
        var user = new User(
            userId, email, name, Set.of(STUDENT), SUSPENDED
        );
        var command = new SuspendUser(userId);

        // When
        var result = user.process(command);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError())
            .isEqualTo(USER_ALREADY_SUSPENDED);
    }
}
```

### Test Coverage Goals

| Aspect | Coverage Target |
|--------|-----------------|
| Each command path | 100% |
| Each business rule | 100% |
| Each error case | 100% |
| Event application | 100% |

## Common Mistakes

### ❌ Don't: Use exceptions

```java
// WRONG
public User process(SuspendUser command) {
    if (status == SUSPENDED) {
        throw new IllegalStateException("Already suspended");
    }
    return new User(...);
}
```

### ✅ Do: Use Result type

```java
// CORRECT
public Result<UserEvents, UserError> process(SuspendUser command) {
    if (status == SUSPENDED) {
        return Result.failure(USER_ALREADY_SUSPENDED);
    }
    return Result.success(new UserSuspended(...));
}
```

### ❌ Don't: Mutate state

```java
// WRONG
public class User {
    private UserStatus status;
    public UserEvents suspend() {
        this.status = SUSPENDED;
        return new UserSuspended(...);
    }
}
```

### ✅ Do: Return new instance

```java
// CORRECT
public record User(UserStatus status, ...) {
    public Result<UserEvents, UserError> process(SuspendUser c) {
        return Result.success(new UserSuspended(...));
    }

    public User apply(UserSuspended event) {
        return new User(SUSPENDED, ...);
    }
}
```

### ❌ Don't: Inject dependencies

```java
// WRONG
public record User(
    UserId id,
    @Inject UserRepository repo  // NO!
) {}
```

### ✅ Do: Keep domain pure

```java
// CORRECT - domain knows nothing about repos
public record User(UserId id, ...) {
    public Result<UserEvents, UserError> process(Command c) {
        // Pure business logic only
    }
}
```

## Checklist

When creating a new domain model:

- [ ] Entity is a `record`
- [ ] Entity has unique identifier
- [ ] `process()` method returns `Result<Event, Error>`
- [ ] Value objects validate in static factory
- [ ] Value objects are `record` type
- [ ] Commands are sealed interface of records
- [ ] Events are sealed interface of records
- [ ] Errors are sealed interface with codes
- [ ] No dependencies on infrastructure
- [ ] No use of exceptions for business logic
- [ ] All business rules are tested

---

**Related:**
- [Commands Events Guide](./05-Commands-Events-Guide.md)
- [Error Handling Guide](./06-Error-Handling-Guide.md)
- [Testing Strategies](./10-Testing-Strategies.md)
