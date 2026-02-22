# Domain-Centric Architecture Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the domain-centric architecture framework as a company-wide template, using the User bounded context as the reference implementation.

**Architecture:** Ports and Adapters (Hexagonal) with Railway Oriented Programming (ROP). Domain layer is pure with zero dependencies on outer layers. Commands flow through Application Services to Domain Entities, which return Events. Events are persisted to Event Store (PostgreSQL). All errors are domain-specific with codes; no exceptions for business logic.

**Tech Stack:** Java 25, Quarkus 3.31, jOOQ, PostgreSQL, JUnit 5, AssertJ, Mockito, ArchUnit, Lombok, Spotless

---

## Task 1: Create Common Module - Result Type

**Files:**
- Create: `src/main/java/com/k12/common/domain/model/Result.java`
- Test: `src/test/java/com/k12/common/domain/model/ResultTest.java`

**Step 1: Write the failing test**

Create `src/test/java/com/k12/common/domain/model/ResultTest.java`:

```java
package com.k12.common.domain.model;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class ResultTest {

    @Test
    @DisplayName("Success result should return value")
    void successResultShouldReturnValue() {
        Result<String, String> result = Result.success("test");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.get()).isEqualTo("test");
    }

    @Test
    @DisplayName("Failure result should return error")
    void failureResultShouldReturnError() {
        Result<String, String> result = Result.failure("error");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("error");
    }

    @Test
    @DisplayName("Map should transform success value")
    void mapShouldTransformSuccessValue() {
        Result<Integer, String> result = Result.success(5);
        Result<String, String> mapped = result.map(i -> "value:" + i);
        assertThat(mapped.get()).isEqualTo("value:5");
    }

    @Test
    @DisplayName("Map should not transform failure")
    void mapShouldNotTransformFailure() {
        Result<Integer, String> result = Result.failure("error");
        Result<String, String> mapped = result.map(i -> "value:" + i);
        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.getError()).isEqualTo("error");
    }

    @Test
    @DisplayName("FlatMap should chain Results")
    void flatMapShouldChainResults() {
        Result<Integer, String> result = Result.success(5);
        Result<String, String> chained = result.flatMap(i -> Result.success("x:" + i));
        assertThat(chained.get()).isEqualTo("x:5");
    }

    @Test
    @DisplayName("FlatMap should short-circuit on failure")
    void flatMapShouldShortCircuitOnFailure() {
        Result<Integer, String> result = Result.failure("error");
        Result<String, String> chained = result.flatMap(i -> Result.success("x:" + i));
        assertThat(chained.isFailure()).isTrue();
    }

    @Test
    @DisplayName("Peek should execute side effect on success")
    void peekShouldExecuteSideEffectOnSuccess() {
        Result<Integer, String> result = Result.success(5);
        int[] counter = {0};
        Result<Integer, String> peeked = result.peek(i -> counter[0]++);
        assertThat(counter[0]).isEqualTo(1);
        assertThat(peeked.get()).isEqualTo(5);
    }

    @Test
    @DisplayName("Peek should not execute on failure")
    void peekShouldNotExecuteOnFailure() {
        Result<Integer, String> result = Result.failure("error");
        int[] counter = {0};
        Result<Integer, String> peeked = result.peek(i -> counter[0]++);
        assertThat(counter[0]).isEqualTo(0);
    }

    @Test
    @DisplayName("GetOrElse should return value on success")
    void getOrElseShouldReturnValueOnSuccess() {
        Result<String, String> result = Result.success("actual");
        assertThat(result.getOrElse("default")).isEqualTo("actual");
    }

    @Test
    @DisplayName("GetOrElse should return default on failure")
    void getOrElseShouldReturnDefaultOnFailure() {
        Result<String, String> result = Result.failure("error");
        assertThat(result.getOrElse("default")).isEqualTo("default");
    }

    @Test
    @DisplayName("Combine should combine two Results")
    void combineShouldCombineTwoResults() {
        Result<Integer, String> r1 = Result.success(5);
        Result<String, String> r2 = Result.success("test");
        Result<String, String> combined = r1.combine(r2, (i, s) -> s + ":" + i);
        assertThat(combined.get()).isEqualTo("test:5");
    }

    @Test
    @DisplayName("Combine should fail if first Result fails")
    void combineShouldFailIfFirstResultFails() {
        Result<Integer, String> r1 = Result.failure("error1");
        Result<String, String> r2 = Result.success("test");
        Result<String, String> combined = r1.combine(r2, (i, s) -> s + ":" + i);
        assertThat(combined.isFailure()).isTrue();
        assertThat(combined.getError()).isEqualTo("error1");
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests ResultTest`

Expected: FAIL with "class Result not found" or similar compilation errors

**Step 3: Write minimal implementation**

Create `src/main/java/com/k12/common/domain/model/Result.java`:

```java
package com.k12.common.domain.model;

/**
 * Railway Oriented Programming Result type.
 * Represents either a success with a value or a failure with an error.
 *
 * @param <T> The success type
 * @param <E> The error type
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    /**
     * Creates a successful result containing the given value.
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed result containing the given error.
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * Returns true if this is a success, false otherwise.
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Returns true if this is a failure, false otherwise.
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Maps the success value if present, using the given function.
     * If this is a failure, returns the same failure.
     */
    @SuppressWarnings("unchecked")
    default <U> Result<U, E> map(java.util.function.Function<T, U> mapper) {
        if (isSuccess()) {
            T value = ((Success<T, E>) this).value();
            try {
                return Result.success(mapper.apply(value));
            } catch (Exception e) {
                return Result.failure((E) e);
            }
        }
        return (Result<U, E>) this;
    }

    /**
     * FlatMaps the success value if present, using the given function.
     * If this is a failure, returns the same failure.
     */
    @SuppressWarnings("unchecked")
    default <U> Result<U, E> flatMap(java.util.function.Function<T, Result<U, E>> mapper) {
        if (isSuccess()) {
            T value = ((Success<T, E>) this).value();
            try {
                return mapper.apply(value);
            } catch (Exception e) {
                return Result.failure((E) e);
            }
        }
        return (Result<U, E>) this;
    }

    /**
     * Execute side effect on success, returning the same result.
     */
    default Result<T, E> peek(java.util.function.Consumer<T> consumer) {
        if (isSuccess()) {
            consumer.accept(((Success<T, E>) this).value());
        }
        return this;
    }

    /**
     * Execute side effect on failure, returning the same result.
     */
    default Result<T, E> peekError(java.util.function.Consumer<E> consumer) {
        if (isFailure()) {
            consumer.accept(((Failure<T, E>) this).error());
        }
        return this;
    }

    /**
     * Combine two Results using a combiner function.
     * Returns failure if either Result is a failure.
     */
    default <U, R> Result<R, E> combine(
            Result<U, E> other,
            java.util.function.BiFunction<T, U, R> combiner) {

        if (isFailure()) return Result.failure(getError());
        if (other.isFailure()) return Result.failure(other.getError());

        return Result.success(combiner.apply(get(), other.get()));
    }

    /**
     * Gets the success value if present, throws otherwise.
     */
    default T get() {
        if (isSuccess()) {
            return ((Success<T, E>) this).value();
        }
        throw new IllegalStateException("Cannot get value from failure: " + ((Failure<T, E>) this).error());
    }

    /**
     * Gets the error if present, throws otherwise.
     */
    default E getError() {
        if (isFailure()) {
            return ((Failure<T, E>) this).error();
        }
        throw new IllegalStateException("Cannot get error from success");
    }

    /**
     * Returns the success value if present, or the given default value.
     */
    default T getOrElse(T defaultValue) {
        if (isSuccess()) {
            return ((Success<T, E>) this).value();
        }
        return defaultValue;
    }

    /**
     * A successful result containing a value.
     */
    record Success<T, E>(T value) implements Result<T, E> {}

    /**
     * A failed result containing an error.
     */
    record Failure<T, E>(E error) implements Result<T, E> {}
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests ResultTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/k12/common/domain/model/Result.java \
        src/test/java/com/k12/common/domain/model/ResultTest.java
git commit -m "feat(common): add Result type for Railway Oriented Programming

- Sealed interface with Success and Failure records
- map, flatMap, peek, peekError combinators
- combine method for aggregating Results

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Create Common ID Types

**Files:**
- Create: `src/main/java/com/k12/common/domain/model/UserId.java`
- Create: `src/main/java/com/k12/common/domain/model/TenantId.java`
- Test: `src/test/java/com/k12/common/domain/model/UserIdTest.java`

**Step 1: Write the failing test**

Create `src/test/java/com/k12/common/domain/model/UserIdTest.java`:

```java
package com.k12.common.domain.model;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class UserIdTest {

    @Test
    @DisplayName("generate should create unique ID")
    void generateShouldCreateUniqueId() {
        UserId id1 = UserId.generate();
        UserId id2 = UserId.generate();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("of should create ID from string")
    void ofShouldCreateIdFromString() {
        String value = "user-123";
        UserId id = UserId.of(value);
        assertThat(id.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("equals should work correctly")
    void equalsShouldWorkCorrectly() {
        String value = "user-123";
        UserId id1 = UserId.of(value);
        UserId id2 = UserId.of(value);
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("toString should return ID value")
    void toStringShouldReturnIdValue() {
        String value = "user-123";
        UserId id = UserId.of(value);
        assertThat(id.toString()).isEqualTo(value);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests UserIdTest`

Expected: FAIL with "class UserId not found"

**Step 3: Write minimal implementation**

Create `src/main/java/com/k12/common/domain/model/UserId.java`:

```java
package com.k12.common.domain.model;

import java.util.UUID;

/**
 * Unique identifier for a User entity.
 */
public record UserId(String value) {

    public static UserId generate() {
        return new UserId("user-" + UUID.randomUUID());
    }

    public static UserId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId value cannot be null or blank");
        }
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

Create `src/main/java/com/k12/common/domain/model/TenantId.java`:

```java
package com.k12.common.domain.model;

import java.util.UUID;

/**
 * Unique identifier for a Tenant entity.
 */
public record TenantId(String value) {

    public static TenantId generate() {
        return new TenantId("tenant-" + UUID.randomUUID());
    }

    public static TenantId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantId value cannot be null or blank");
        }
        return new TenantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests UserIdTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/k12/common/domain/model/UserId.java \
        src/main/java/com/k12/common/domain/model/TenantId.java \
        src/test/java/com/k12/common/domain/model/UserIdTest.java
git commit -m "feat(common): add UserId and TenantId value objects

- Generate unique IDs with UUID
- Static factory methods with validation
- Proper equals/hashCode via record

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Create User Domain Layer - Commands

**Files:**
- Create: `src/main/java/com/k12/user/domain/model/commands/UserCommands.java`
- Test: `src/test/java/com/k12/user/domain/model/commands/UserCommandsTest.java`

**Step 1: Write the failing test**

Create `src/test/java/com/k12/user/domain/model/commands/UserCommandsTest.java`:

```java
package com.k12.user.domain.model.commands;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.EmailAddress;
import com.k12.user.domain.model.UserName;
import com.k12.user.domain.model.UserRole;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class UserCommandsTest {

    @Test
    @DisplayName("CreateUser command should hold values")
    void createUserCommandShouldHoldValues() {
        var command = new UserCommands.CreateUser(
            "test@example.com",
            "hashedPassword",
            Set.of(UserRole.STUDENT),
            "John Doe"
        );
        assertThat(command.email()).isEqualTo("test@example.com");
        assertThat(command.passwordHash()).isEqualTo("hashedPassword");
        assertThat(command.roles()).containsExactly(UserRole.STUDENT);
        assertThat(command.name()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("SuspendUser command should hold userId")
    void suspendUserCommandShouldHoldUserId() {
        UserId userId = UserId.generate();
        var command = new UserCommands.SuspendUser(userId);
        assertThat(command.userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("ActivateUser command should hold userId")
    void activateUserCommandShouldHoldUserId() {
        UserId userId = UserId.generate();
        var command = new UserCommands.ActivateUser(userId);
        assertThat(command.userId()).isEqualTo(userId);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests UserCommandsTest`

Expected: FAIL with classes not found (need to create UserRole enum first)

**Step 3: Create UserRole enum**

Create `src/main/java/com/k12/user/domain/model/UserRole.java`:

```java
package com.k12.user.domain.model;

/**
 * User role enumeration.
 */
public enum UserRole {
    ADMIN,
    TEACHER,
    STUDENT,
    PARENT
}
```

**Step 4: Write UserCommands implementation**

Create `src/main/java/com/k12/user/domain/model/commands/UserCommands.java`:

```java
package com.k12.user.domain.model.commands;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.UserRole;
import java.util.Set;

/**
 * Sealed interface representing all possible user commands.
 * Each command is a record containing the data needed to execute it.
 */
public sealed interface UserCommands
        permits UserCommands.CreateUser,
                UserCommands.SuspendUser,
                UserCommands.ActivateUser {

    /**
     * Command to create a new user.
     */
    record CreateUser(
            String email,
            String passwordHash,
            Set<UserRole> roles,
            String name
    ) implements UserCommands {}

    /**
     * Command to suspend a user.
     */
    record SuspendUser(UserId userId) implements UserCommands {}

    /**
     * Command to activate a user.
     */
    record ActivateUser(UserId userId) implements UserCommands {}
}
```

**Step 5: Run test to verify it passes**

Run: `./gradlew test --tests UserCommandsTest`

Expected: PASS

**Step 6: Commit**

```bash
git add src/main/java/com/k12/user/domain/model/UserRole.java \
        src/main/java/com/k12/user/domain/model/commands/UserCommands.java \
        src/test/java/com/k12/user/domain/model/commands/UserCommandsTest.java
git commit -m "feat(user): add UserRole enum and UserCommands

- Sealed interface for type-safe commands
- CreateUser, SuspendUser, ActivateUser commands
- Records for immutable command data

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Create User Domain Layer - Events

**Files:**
- Create: `src/main/java/com/k12/user/domain/model/events/UserEvents.java`
- Test: `src/test/java/com/k12/user/domain/model/events/UserEventsTest.java`

**Step 1: Write the failing test**

Create `src/test/java/com/k12/user/domain/model/events/UserEventsTest.java`:

```java
package com.k12.user.domain.model.events;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.EmailAddress;
import com.k12.user.domain.model.UserName;
import com.k12.user.domain.model.UserRole;
import com.k12.user.domain.model.UserStatus;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class UserEventsTest {

    @Test
    @DisplayName("UserCreated event should hold all values")
    void userCreatedEventShouldHoldAllValues() {
        UserId userId = UserId.generate();
        EmailAddress email = EmailAddress.of("test@example.com").get();
        UserName name = UserName.of("John Doe").get();
        Set<UserRole> roles = Set.of(UserRole.STUDENT);
        Instant createdAt = Instant.now();

        var event = new UserEvents.UserCreated(
            userId, email, "hash", roles, UserStatus.ACTIVE, name, createdAt, 1L
        );

        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.email()).isEqualTo(email);
        assertThat(event.name()).isEqualTo(name);
        assertThat(event.roles()).isEqualTo(roles);
        assertThat(event.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("UserSuspended event should hold userId")
    void userSuspendedEventShouldHoldUserId() {
        UserId userId = UserId.generate();
        Instant suspendedAt = Instant.now();

        var event = new UserEvents.UserSuspended(userId, suspendedAt, 2L);

        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.version()).isEqualTo(2L);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests UserEventsTest`

Expected: FAIL with classes not found (need UserStatus enum and value objects)

**Step 3: Create UserStatus enum**

Create `src/main/java/com/k12/user/domain/model/UserStatus.java`:

```java
package com.k12.user.domain.model;

/**
 * User status enumeration.
 */
public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    PENDING_ACTIVATION
}
```

**Step 4: Create EmailAddress value object**

Create `src/main/java/com/k12/user/domain/model/EmailAddress.java`:

```java
package com.k12.user.domain.model;

import com.k12.common.domain.model.Result;
import java.util.regex.Pattern;

/**
 * Email address value object.
 */
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

**Step 5: Create UserName value object**

Create `src/main/java/com/k12/user/domain/model/UserName.java`:

```java
package com.k12.user.domain.model;

import com.k12.common.domain.model.Result;

/**
 * User name value object.
 */
public record UserName(String value) {

    public static Result<UserName, String> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Name cannot be null or blank");
        }
        if (value.length() < 2) {
            return Result.failure("Name must be at least 2 characters");
        }
        if (value.length() > 100) {
            return Result.failure("Name must be less than 100 characters");
        }
        return Result.success(new UserName(value));
    }
}
```

**Step 6: Create PasswordHash value object**

Create `src/main/java/com/k12/user/domain/model/PasswordHash.java`:

```java
package com.k12.user.domain.model;

/**
 * Password hash value object.
 */
public record PasswordHash(String value) {

    public static PasswordHash of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }
        return new PasswordHash(value);
    }
}
```

**Step 7: Write UserEvents implementation**

Create `src/main/java/com/k12/user/domain/model/events/UserEvents.java`:

```java
package com.k12.user.domain.model.events;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.EmailAddress;
import com.k12.user.domain.model.UserName;
import com.k12.user.domain.model.UserRole;
import com.k12.user.domain.model.UserStatus;
import java.time.Instant;
import java.util.Set;

/**
 * Domain events representing state changes in the User aggregate.
 * Events contain ONLY the changed data (deltas), not full state.
 */
public sealed interface UserEvents
        permits UserEvents.UserCreated,
                UserEvents.UserSuspended,
                UserEvents.UserActivated {

    /**
     * User was created - contains initial state.
     */
    record UserCreated(
            UserId userId,
            EmailAddress email,
            String passwordHash,
            Set<UserRole> roles,
            UserStatus status,
            UserName name,
            Instant createdAt,
            long version
    ) implements UserEvents {}

    /**
     * User was suspended.
     */
    record UserSuspended(UserId userId, Instant suspendedAt, long version) implements UserEvents {}

    /**
     * User was activated.
     */
    record UserActivated(UserId userId, Instant activatedAt, long version) implements UserEvents {}
}
```

**Step 8: Run test to verify it passes**

Run: `./gradlew test --tests UserEventsTest`

Expected: PASS

**Step 9: Commit**

```bash
git add src/main/java/com/k12/user/domain/model/UserStatus.java \
        src/main/java/com/k12/user/domain/model/EmailAddress.java \
        src/main/java/com/k12/user/domain/model/UserName.java \
        src/main/java/com/k12/user/domain/model/PasswordHash.java \
        src/main/java/com/k12/user/domain/model/events/UserEvents.java \
        src/test/java/com/k12/user/domain/model/events/UserEventsTest.java
git commit -m "feat(user): add domain events and value objects

- Sealed UserEvents interface
- UserCreated, UserSuspended, UserActivated events
- EmailAddress, UserName, PasswordHash value objects with validation
- UserStatus enum

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Create User Domain Layer - Errors

**Files:**
- Create: `src/main/java/com/k12/user/domain/model/error/UserError.java`
- Test: `src/test/java/com/k12/user/domain/model/error/UserErrorTest.java`

**Step 1: Write the failing test**

Create `src/test/java/com/k12/user/domain/model/error/UserErrorTest.java`:

```java
package com.k12.user.domain.model.error;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class UserErrorTest {

    @Test
    @DisplayName("ValidationError should have code and message")
    void validationErrorShouldHaveCodeAndMessage() {
        var error = UserError.ValidationError.INVALID_EMAIL_FORMAT;
        assertThat(error.code()).isEqualTo("INVALID_EMAIL_FORMAT");
        assertThat(error.message()).isNotBlank();
    }

    @Test
    @DisplayName("ConflictError should have code and message")
    void conflictErrorShouldHaveCodeAndMessage() {
        var error = UserError.ConflictError.EMAIL_ALREADY_IN_USE;
        assertThat(error.code()).isEqualTo("EMAIL_ALREADY_IN_USE");
        assertThat(error.message()).isNotBlank();
    }

    @Test
    @DisplayName("PreConditionError should have code and message")
    void preConditionErrorShouldHaveCodeAndMessage() {
        var error = UserError.PreConditionError.USER_ALREADY_SUSPENDED;
        assertThat(error.code()).isEqualTo("USER_ALREADY_SUSPENDED");
        assertThat(error.message()).isNotBlank();
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests UserErrorTest`

Expected: FAIL with "class UserError not found"

**Step 3: Write minimal implementation**

Create `src/main/java/com/k12/user/domain/model/error/UserError.java`:

```java
package com.k12.user.domain.model.error;

import java.util.Map;

/**
 * Domain errors for User bounded context.
 * All errors are domain-specific with codes and metadata.
 */
public sealed interface UserError permits
        UserError.ValidationError,
        UserError.ConflictError,
        UserError.PreConditionError,
        UserError.DomainError,
        UserError.ConcurrencyError,
        UserError.PersistenceError {

    String code();
    String message();
    Map<String, Object> metadata();

    /**
     * Validation errors (bad user input) - HTTP 400
     */
    public record ValidationError(
            String code,
            String message,
            Map<String, Object> metadata
    ) implements UserError {

        public static final ValidationError INVALID_EMAIL_FORMAT = new ValidationError(
            "INVALID_EMAIL_FORMAT",
            "Email address format is invalid",
            Map.of("field", "email")
        );

        public static final ValidationError PASSWORD_TOO_WEAK = new ValidationError(
            "PASSWORD_TOO_WEAK",
            "Password must be at least 12 characters with uppercase, lowercase, numbers",
            Map.of("field", "password", "minLength", 12)
        );
    }

    /**
     * Conflict errors (resource already exists) - HTTP 409
     */
    public record ConflictError(
            String code,
            String message,
            Map<String, Object> metadata
    ) implements UserError {

        public static final ConflictError EMAIL_ALREADY_IN_USE = new ConflictError(
            "EMAIL_ALREADY_IN_USE",
            "An account with this email already exists",
            Map.of("resource", "user", "field", "email")
        );
    }

    /**
     * Pre-condition errors (business rule violations) - HTTP 422
     */
    public record PreConditionError(
            String code,
            String message,
            Map<String, Object> metadata
    ) implements UserError {

        public static final PreConditionError USER_ALREADY_SUSPENDED = new PreConditionError(
            "USER_ALREADY_SUSPENDED",
            "User is already suspended",
            Map.of("field", "status")
        );

        public static final PreConditionError USER_ALREADY_ACTIVE = new PreConditionError(
            "USER_ALREADY_ACTIVE",
            "User is already active",
            Map.of("field", "status")
        );
    }

    /**
     * Domain errors (business logic violations) - HTTP 422
     */
    public record DomainError(
            String code,
            String message,
            Map<String, Object> metadata
    ) implements UserError {

        public static final DomainError CANNOT_REMOVE_LAST_ROLE = new DomainError(
            "CANNOT_REMOVE_LAST_ROLE",
            "User must have at least one role",
            Map.of("minRoles", 1)
        );
    }

    /**
     * Concurrency errors (optimistic locking) - HTTP 409
     */
    public record ConcurrencyError(
            String code,
            String message,
            Map<String, Object> metadata
    ) implements UserError {

        public static final ConcurrencyError VERSION_CONFLICT = new ConcurrencyError(
            "VERSION_CONFLICT",
            "Resource was modified by another transaction. Please retry.",
            Map.of("retryable", true)
        );
    }

    /**
     * Persistence errors (database/storage issues) - HTTP 500
     */
    public record PersistenceError(
            String code,
            String message,
            Map<String, Object> metadata
    ) implements UserError {

        public static final PersistenceError STORAGE_ERROR = new PersistenceError(
            "STORAGE_ERROR",
            "Unable to store data. Please try again.",
            Map.of("retryable", true)
        );
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests UserErrorTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/k12/user/domain/model/error/UserError.java \
        src/test/java/com/k12/user/domain/model/error/UserErrorTest.java
git commit -m "feat(user): add domain error types

- Sealed interface for type-safe errors
- ValidationError, ConflictError, PreConditionError
- DomainError, ConcurrencyError, PersistenceError
- Error codes and metadata for HTTP mapping

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Create User Entity

**Files:**
- Create: `src/main/java/com/k12/user/domain/model/User.java`
- Test: `src/test/java/com/k12/user/domain/model/UserTest.java`

**Step 1: Write the failing test**

Create `src/test/java/com/k12/user/domain/model/UserTest.java`:

```java
package com.k12.user.domain.model;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.commands.UserCommands;
import com.k12.user.domain.model.events.UserEvents;
import com.k12.user.domain.model.error.UserError;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Suspend active user should return UserSuspended event")
    void suspendActiveUserShouldReturnEvent() {
        var user = createUser(UserStatus.ACTIVE);
        var command = new UserCommands.SuspendUser(user.userId());

        var result = user.process(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isInstanceOf(UserEvents.UserSuspended.class);
    }

    @Test
    @DisplayName("Suspend already suspended user should return error")
    void suspendAlreadySuspendedUserShouldReturnError() {
        var user = createUser(UserStatus.SUSPENDED);
        var command = new UserCommands.SuspendUser(user.userId());

        var result = user.process(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError())
            .isEqualTo(UserError.PreConditionError.USER_ALREADY_SUSPENDED);
    }

    @Test
    @DisplayName("Activate suspended user should return UserActivated event")
    void activateSuspendedUserShouldReturnEvent() {
        var user = createUser(UserStatus.SUSPENDED);
        var command = new UserCommands.ActivateUser(user.userId());

        var result = user.process(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isInstanceOf(UserEvents.UserActivated.class);
    }

    @Test
    @DisplayName("Activate already active user should return error")
    void activateAlreadyActiveUserShouldReturnError() {
        var user = createUser(UserStatus.ACTIVE);
        var command = new UserCommands.ActivateUser(user.userId());

        var result = user.process(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError())
            .isEqualTo(UserError.PreConditionError.USER_ALREADY_ACTIVE);
    }

    private User createUser(UserStatus status) {
        return new User(
            UserId.generate(),
            EmailAddress.of("test@example.com").get(),
            PasswordHash.of("hash"),
            Set.of(UserRole.STUDENT),
            status,
            UserName.of("Test User").get()
        );
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests UserTest`

Expected: FAIL with "class User not found"

**Step 3: Write minimal implementation**

Create `src/main/java/com/k12/user/domain/model/User.java`:

```java
package com.k12.user.domain.model;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.commands.UserCommands;
import com.k12.user.domain.model.error.UserError;
import com.k12.user.domain.model.events.UserEvents;
import java.util.Set;

import static com.k12.user.domain.model.error.UserError.PreConditionError.*;
import static java.time.Instant.now;

/**
 * User aggregate root.
 * Processes commands and returns events.
 */
public record User(
        UserId userId,
        EmailAddress emailAddress,
        PasswordHash passwordHash,
        Set<UserRole> roles,
        UserStatus status,
        UserName name
) {

    /**
     * Process a command and return an event or error.
     */
    public Result<UserEvents, UserError> process(UserCommands command) {
        return switch (command) {
            case UserCommands.SuspendUser c -> process(c);
            case UserCommands.ActivateUser c -> process(c);
            default -> throw new IllegalStateException("Unexpected command: " + command);
        };
    }

    /**
     * Suspend the user if not already suspended.
     */
    private Result<UserEvents, UserError> process(UserCommands.SuspendUser command) {
        if (this.status == UserStatus.SUSPENDED) {
            return Result.failure(USER_ALREADY_SUSPENDED);
        }
        return Result.success(new UserEvents.UserSuspended(
            this.userId(), now(), System.currentTimeMillis()
        ));
    }

    /**
     * Activate the user if not already active.
     */
    private Result<UserEvents, UserError> process(UserCommands.ActivateUser command) {
        if (this.status == UserStatus.ACTIVE) {
            return Result.failure(USER_ALREADY_ACTIVE);
        }
        return Result.success(new UserEvents.UserActivated(
            this.userId(), now(), System.currentTimeMillis()
        ));
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests UserTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/k12/user/domain/model/User.java \
        src/test/java/com/k12/user/domain/model/UserTest.java
git commit -m "feat(user): add User entity with command processing

- Record-based immutable entity
- Process commands returning Result<Event, Error>
- SuspendUser and ActivateUser command handlers
- Status invariant validation

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 7: Create UserFactory and UserReconstructor

**Files:**
- Create: `src/main/java/com/k12/user/domain/model/UserFactory.java`
- Create: `src/main/java/com/k12/user/domain/model/UserReconstructor.java`
- Test: `src/test/java/com/k12/user/domain/model/UserFactoryTest.java`

**Step 1: Write the failing test**

Create `src/test/java/com/k12/user/domain/model/UserFactoryTest.java`:

```java
package com.k12.user.domain.model;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.commands.UserCommands;
import com.k12.user.domain.model.events.UserEvents;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class UserFactoryTest {

    @Test
    @DisplayName("createNewUser should create UserCreated event")
    void createNewUserShouldCreateEvent() {
        var command = new UserCommands.CreateUser(
            "test@example.com",
            "hashedPassword",
            Set.of(UserRole.STUDENT),
            "John Doe"
        );

        var result = UserFactory.createNewUser(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isInstanceOf(UserEvents.UserCreated.class);

        var event = (UserEvents.UserCreated) result.get();
        assertThat(event.email().value()).isEqualTo("test@example.com");
        assertThat(event.name().value()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("reconstructFromEvents should build current state")
    void reconstructFromEventsShouldBuildCurrentState() {
        var userId = UserId.generate();
        var created = new UserEvents.UserCreated(
            userId,
            EmailAddress.of("test@example.com").get(),
            "hash",
            Set.of(UserRole.STUDENT),
            UserStatus.ACTIVE,
            UserName.of("Test User").get(),
            Instant.now(),
            1L
        );
        var suspended = new UserEvents.UserSuspended(userId, Instant.now(), 2L);

        var user = UserReconstructor.reconstruct(List.of(created, suspended));

        assertThat(user.status()).isEqualTo(UserStatus.SUSPENDED);
    }
}
```

**Step 3: Write implementation**

Create `src/main/java/com/k12/user/domain/model/UserFactory.java`:

```java
package com.k12.user.domain.model;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.commands.UserCommands;
import com.k12.user.domain.model.error.UserError;
import com.k12.user.domain.model.events.UserEvents;
import java.time.Instant;
import java.util.Set;

/**
 * Factory for creating User entities and events.
 */
public final class UserFactory {

    /**
     * Create a new User entity from a CreateUser command.
     * Returns UserCreated event.
     */
    public static Result<UserEvents.UserCreated, UserError> createNewUser(
            UserCommands.CreateUser command) {

        // Validate and create value objects
        var emailResult = EmailAddress.of(command.email());
        var nameResult = UserName.of(command.name());

        if (emailResult.isFailure()) {
            return Result.failure(UserError.ValidationError.INVALID_EMAIL_FORMAT);
        }
        if (nameResult.isFailure()) {
            return Result.failure(new UserError.ValidationError(
                "INVALID_NAME",
                nameResult.getError(),
                java.util.Map.of("field", "name")
            ));
        }

        var userId = UserId.generate();
        return Result.success(new UserEvents.UserCreated(
            userId,
            emailResult.get(),
            command.passwordHash(),
            command.roles(),
            UserStatus.PENDING_ACTIVATION,
            nameResult.get(),
            Instant.now(),
            1L
        ));
    }

    /**
     * Create initial empty user state (for new user before creation).
     */
    public static User createInitialState() {
        return new User(
            null,
            null,
            null,
            Set.of(),
            null,
            null
        );
    }

    private UserFactory() {}
}
```

Create `src/main/java/com/k12/user/domain/model/UserReconstructor.java`:

```java
package com.k12.user.domain.model;

import com.k12.user.domain.model.events.UserEvents;
import java.util.List;

/**
 * Reconstructs User entity from events.
 */
public final class UserReconstructor {

    /**
     * Reconstruct User state from a list of events.
     */
    public static User reconstruct(List<UserEvents> events) {
        User user = null;

        for (UserEvents event : events) {
            if (event instanceof UserEvents.UserCreated created) {
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

    /**
     * Apply a single event to a User, returning the updated User.
     */
    public static User applyEvent(User user, UserEvents event) {
        return switch (event) {
            case UserEvents.UserSuspended e ->
                new User(user.userId(), user.emailAddress(), user.passwordHash(),
                         user.roles(), UserStatus.SUSPENDED, user.name());

            case UserEvents.UserActivated e ->
                new User(user.userId(), user.emailAddress(), user.passwordHash(),
                         user.roles(), UserStatus.ACTIVE, user.name());

            default -> user;
        };
    }

    private UserReconstructor() {}
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests UserFactoryTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/k12/user/domain/model/UserFactory.java \
        src/main/java/com/k12/user/domain/model/UserReconstructor.java \
        src/test/java/com/k12/user/domain/model/UserFactoryTest.java
git commit -m "feat(user): add UserFactory and UserReconstructor

- UserFactory creates new users from commands
- UserReconstructor rebuilds state from events
- Initial state creation for new users

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Create Domain Ports

**Files:**
- Create: `src/main/java/com/k12/user/domain/port/input/UserRepository.java`
- Create: `src/main/java/com/k12/user/domain/port/output/EventStore.java`
- Create: `src/main/java/com/k12/user/domain/port/output/ProjectionUpdater.java`
- Create: `src/main/java/com/k12/user/domain/port/output/SideEffectTrigger.java`

**Step 1: Create UserRepository port**

Create `src/main/java/com/k12/user/domain/port/input/UserRepository.java`:

```java
package com.k12.user.domain.port.input;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.User;
import com.k12.user.domain.model.error.UserError;

/**
 * Port for User repository operations.
 * Implemented by infrastructure layer.
 */
public interface UserRepository {

    /**
     * Find user by ID.
     */
    Result<User, UserError> findById(UserId userId);

    /**
     * Check if email exists.
     */
    boolean emailExists(com.k12.user.domain.model.EmailAddress email);

    /**
     * Check if tenant is active.
     */
    boolean tenantIsActive(String tenantId);
}
```

**Step 2: Create EventStore port**

Create `src/main/java/com/k12/user/domain/port/output/EventStore.java`:

```java
package com.k12.user.domain.port.output;

import com.k12.common.domain.model.Result;
import com.k12.user.domain.model.error.UserError;
import com.k12.user.domain.model.events.UserEvents;
import java.util.List;

/**
 * Port for event store operations.
 * Implemented by infrastructure layer.
 */
public interface EventStore {

    /**
     * Append a single event to the event store.
     */
    Result<PersistedEvent, UserError> append(UserEvents event);

    /**
     * Append multiple events atomically.
     */
    Result<List<PersistedEvent>, UserError> appendAll(List<UserEvents> events);

    /**
     * Load all events for a user.
     */
    Result<List<UserEvents>, UserError> load(String userId);

    /**
     * Represents a successfully persisted event.
     */
    record PersistedEvent(
        UserEvents event,
        long version,
        java.time.Instant persistedAt
    ) {}
}
```

**Step 3: Create ProjectionUpdater port**

Create `src/main/java/com/k12/user/domain/port/output/ProjectionUpdater.java`:

```java
package com.k12.user.domain.port.output;

import com.k12.common.domain.model.Result;
import com.k12.user.domain.model.error.UserError;
import com.k12.user.domain.port.output.EventStore.PersistedEvent;

/**
 * Port for updating read projections.
 * Implemented by infrastructure layer.
 */
public interface ProjectionUpdater {

    /**
     * Update projection for a single event.
     */
    Result<Void, UserError> update(PersistedEvent event);

    /**
     * Update projections for multiple events.
     */
    Result<Void, UserError> updateAll(java.util.List<PersistedEvent> events);
}
```

**Step 4: Create SideEffectTrigger port**

Create `src/main/java/com/k12/user/domain/port/output/SideEffectTrigger.java`:

```java
package com.k12.user.domain.port.output;

import com.k12.user.domain.model.events.UserEvents;

/**
 * Port for triggering asynchronous side effects.
 * Implemented by infrastructure layer.
 */
public interface SideEffectTrigger {

    /**
     * Trigger side effect asynchronously (fire-and-forget).
     */
    void triggerAsync(UserEvents event);

    /**
     * Trigger side effect synchronously.
     */
    void trigger(UserEvents event);
}
```

**Step 5: Commit**

```bash
git add src/main/java/com/k12/user/domain/port/
git commit -m "feat(user): add domain ports

- UserRepository (input port)
- EventStore, ProjectionUpdater, SideEffectTrigger (output ports)
- Clear separation between domain and infrastructure

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 9: Create Application Service - CommandHandler

**Files:**
- Create: `src/main/java/com/k12/user/application/service/CommandHandler.java`
- Test: `src/test/java/com/k12/user/application/service/CommandHandlerTest.java`

**Step 1: Write the failing test**

Create `src/test/java/com/k12/user/application/service/CommandHandlerTest.java`:

```java
package com.k12.user.application.service;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.model.User;
import com.k12.user.domain.model.UserStatus;
import com.k12.user.domain.model.commands.UserCommands;
import com.k12.user.domain.model.error.UserError;
import com.k12.user.domain.model.events.UserEvents;
import com.k12.user.domain.port.output.EventStore;
import com.k12.user.domain.port.output.EventStore.PersistedEvent;
import com.k12.user.domain.port.output.ProjectionUpdater;
import com.k12.user.domain.port.output.SideEffectTrigger;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandHandlerTest {

    @Mock EventStore eventStore;
    @Mock ProjectionUpdater projectionUpdater;
    @Mock SideEffectTrigger sideEffectTrigger;

    private CommandHandler<User, UserError> commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new CommandHandler<>(
            eventStore, projectionUpdater, sideEffectTrigger
        );
    }

    @Test
    @DisplayName("handle should execute full pipeline successfully")
    void handleShouldExecuteFullPipelineSuccessfully() {
        var user = createUser();
        var event = new UserEvents.UserSuspended(
            user.userId(), Instant.now(), 2L
        );
        var persisted = new PersistedEvent(event, 2L, Instant.now());

        when(eventStore.append(event)).thenReturn(Result.success(persisted));
        when(projectionUpdater.update(persisted)).thenReturn(Result.success(null));

        var result = commandHandler.handle(
            () -> Result.success(user),
            u -> u.process(new UserCommands.SuspendUser(u.userId())),
            e -> "suspended:" + e.event().userId().value()
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).startsWith("suspended:");

        verify(eventStore).append(event);
        verify(projectionUpdater).update(persisted);
        verify(sideEffectTrigger).triggerAsync(persisted);
    }

    @Test
    @DisplayName("handle should fail when loader fails")
    void handleShouldFailWhenLoaderFails() {
        var error = UserError.ValidationError.INVALID_EMAIL_FORMAT;

        var result = commandHandler.handle(
            () -> Result.failure(error),
            u -> u.process(new UserCommands.SuspendUser(u.userId())),
            e -> "result"
        );

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo(error);

        verifyNoInteractions(eventStore, projectionUpdater, sideEffectTrigger);
    }

    private User createUser() {
        return new User(
            UserId.generate(),
            com.k12.user.domain.model.EmailAddress.of("test@example.com").get(),
            com.k12.user.domain.model.PasswordHash.of("hash"),
            java.util.Set.of(com.k12.user.domain.model.UserRole.STUDENT),
            UserStatus.ACTIVE,
            com.k12.user.domain.model.UserName.of("Test").get()
        );
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests CommandHandlerTest`

Expected: FAIL with "class CommandHandler not found"

**Step 3: Write implementation**

Create `src/main/java/com/k12/user/application/service/CommandHandler.java`:

```java
package com.k12.user.application.service;

import com.k12.common.domain.model.Result;
import com.k12.user.domain.model.error.UserError;
import com.k12.user.domain.port.output.EventStore;
import com.k12.user.domain.port.output.ProjectionUpdater;
import com.k12.user.domain.port.output.SideEffectTrigger;
import com.k12.user.domain.port.output.EventStore.PersistedEvent;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Generic command handler that orchestrates the standard flow:
 * Load → Process → Persist → Update Projection → Trigger Side Effects
 */
@ApplicationScoped
public class CommandHandler<T, E> {

    private final EventStore eventStore;
    private final ProjectionUpdater projectionUpdater;
    private final SideEffectTrigger sideEffectTrigger;

    public CommandHandler(
            EventStore eventStore,
            ProjectionUpdater projectionUpdater,
            SideEffectTrigger sideEffectTrigger) {
        this.eventStore = eventStore;
        this.projectionUpdater = projectionUpdater;
        this.sideEffectTrigger = sideEffectTrigger;
    }

    /**
     * Execute a command using the standard flow.
     */
    @jakarta.transaction.Transactional
    public <R> Result<R, E> handle(
            java.util.function.Supplier<Result<T, E>> loader,
            java.util.function.Function<T, Result<com.k12.user.domain.model.events.UserEvents, E>> processor,
            java.util.function.Function<PersistedEvent, R> responseMapper) {

        return loader.get()  // Load entity
            .flatMap(processor)  // Process command
            .flatMap(events ->
                eventStore.append(events)  // Persist events
                    .flatMap(persisted -> {
                        // Update projection (sync, in same transaction)
                        var updateResult = projectionUpdater.update(persisted);
                        if (updateResult.isFailure()) {
                            return (Result<PersistedEvent, E>) updateResult;
                        }

                        // Trigger side effects (async, don't affect result)
                        sideEffectTrigger.triggerAsync(persisted);

                        // Build response
                        return (Result<PersistedEvent, E>) Result.success(persisted);
                    })
            )
            .map(responseMapper);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests CommandHandlerTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/k12/user/application/service/CommandHandler.java \
        src/test/java/com/k12/user/application/service/CommandHandlerTest.java
git commit -m "feat(user): add generic CommandHandler

- Orchestrates load → process → persist → update → trigger
- Transactional by default
- ROP-based error propagation
- Async side effect triggering

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 10: Create Architecture Tests

**Files:**
- Create: `src/test/java/com/k12/user/architecture/UserArchitectureTest.java`

**Step 1: Write architecture test**

Create `src/test/java/com/k12/user/architecture/UserArchitectureTest.java`:

```java
package com.k12.user.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

@AnalyzeClasses(packages = "com.k12.user")
class UserArchitectureTest {

    @ArchTest
    static final ArchRule domain_layer_should_not_depend_on_application =
        classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                    "..domain..",
                    "java..",
                    "org.jetbrains..",
                    "lombok..",
                    "com.k12.common.domain.."
                );

    @ArchTest
    static final ArchRule domain_entities_should_be_records =
        classes()
            .that().haveSimpleName("User")
            .should().beRecords();

    @ArchTest
    static final ArchRule commands_should_be_sealed_interfaces =
        classes()
            .that().haveSimpleNameEndingWith("Commands")
            .should().beInterfaces()
            .andShould()
            .beModifiers()
            .withOnlySealedInterfaces();

    @ArchTest
    static final ArchRule events_should_be_sealed_interfaces =
        classes()
            .that().haveSimpleNameEndingWith("Events")
            .should().beInterfaces()
            .andShould()
            .beModifiers()
            .withOnlySealedInterfaces();

    @ArchTest
    static final ArchRule errors_should_be_sealed_interfaces =
        classes()
            .that().haveSimpleNameEndingWith("Error")
            .should().beInterfaces()
            .andShould()
            .beModifiers()
            .withOnlySealedInterfaces();

    @ArchTest
    static final ArchRule value_objects_should_be_records =
        classes()
            .that().resideInAPackage("..domain.model..")
            .and().areNotAssignableTo(Enum.class)
            .and().areNotAssignableTo(UserCommands.class)
            .and().areNotAssignableTo(UserEvents.class)
            .should().beRecords();
}
```

**Step 2: Run test to verify it passes**

Run: `./gradlew test --tests UserArchitectureTest`

Expected: PASS

**Step 3: Commit**

```bash
git add src/test/java/com/k12/user/architecture/UserArchitectureTest.java
git commit -m "test(user): add ArchUnit tests for architecture compliance

- Domain layer isolation
- Records for entities and value objects
- Sealed interfaces for commands, events, errors
- Enforce dependency rules

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 11: Create README Documentation

**Files:**
- Create: `docs/architecture-overview.md`

**Step 1: Create documentation**

Create `docs/architecture-overview.md`:

```markdown
# Domain-Centric Architecture Overview

This document provides an overview of the domain-centric architecture implemented in this project.

## Key Principles

1. **Domain Centric**: All business logic lives in the domain layer
2. **Ports & Adapters**: Domain defines ports, infrastructure implements adapters
3. **ROP Only**: No exceptions for business logic, always use `Result<T, E>`
4. **Event Sourcing**: Entities are projections of their event stream
5. **Immutable**: All entities and value objects are immutable records
6. **Sealed Types**: Commands, events, errors use sealed interfaces for type safety

## Layer Responsibilities

### Domain Layer (`src/main/java/com/k12/<context>/domain/`)

**Pure business logic, zero dependencies on outer layers.**

- **model/**: Entities, value objects, factory, reconstructor
- **commands/**: Sealed interfaces defining all possible commands
- **events/**: Sealed interfaces defining domain events
- **error/**: Domain-specific errors with codes
- **port/**
  - **input/**: Repository interfaces (what domain needs)
  - **output/**: Event store, projection, side effect interfaces

### Application Layer (`src/main/java/com/k12/<context>/application/`)

**Orchestrates domain logic, uses domain ports.**

- **input/**: DTO validation, command conversion, pre-condition checks
- **service/**: CommandHandler, Pipeline, Saga orchestrators
- **output/**: Coordinates event persistence and projection updates

### Infrastructure Layer (`src/main/java/com/k12/<context>/infrastructure/`)

**Implements ports defined by domain.**

- **driving/**: REST controllers, message consumers, schedulers
- **driven/**: Event store, repositories, external service clients

## Request Flow

```
1. REST Controller receives DTO
2. Input validates DTO, converts to Command
3. Pre-condition checks run
4. Application Service loads entity via Repository
5. Entity processes command → returns Event
6. EventStore persists event
7. ProjectionUpdater updates read model
8. SideEffectTrigger triggers async effects
9. ResponseDTO returned to caller
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

## Testing

- **80% Unit Tests**: Domain layer logic
- **15% Integration Tests**: Adapters and ports
- **5% E2E Tests**: Critical user journeys
- **Architecture Tests**: Enforce dependency rules with ArchUnit

## Further Reading

See [docs/plans/2026-02-22-domain-centric-architecture-design.md](./plans/2026-02-22-domain-centric-architecture-design.md) for complete architecture specification.
```

**Step 2: Commit**

```bash
git add docs/architecture-overview.md
git commit -m "docs: add architecture overview

Quick reference guide for the domain-centric architecture
- Layer responsibilities
- Request flow
- Error handling
- Testing strategy

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 12: Final Verification

**Files:** None (verification only)

**Step 1: Run all tests**

Run: `./gradlew test`

Expected: All tests PASS

**Step 2: Check code formatting**

Run: `./gradlew spotlessCheck`

Expected: No formatting issues

**Step 3: Run architecture tests**

Run: `./gradlew test --tests "*ArchitectureTest"`

Expected: All architecture tests PASS

**Step 4: Verify package structure**

Run: `find src/main/java/com/k12/user -type f -name "*.java" | head -20`

Expected output shows correct package structure:
```
src/main/java/com/k12/user/domain/model/User.java
src/main/java/com/k12/user/domain/model/commands/UserCommands.java
src/main/java/com/k12/user/domain/model/events/UserEvents.java
src/main/java/com/k12/user/domain/model/error/UserError.java
src/main/java/com/k12/user/domain/port/input/UserRepository.java
src/main/java/com/k12/user/domain/port/output/EventStore.java
src/main/java/com/k12/user/application/service/CommandHandler.java
...
```

**Step 5: Final commit**

```bash
git commit --allow-empty -m "feat(user): complete domain-centric architecture framework

Implementation complete:
- Common Result<T, E> type for ROP
- User bounded context as reference implementation
- Domain layer with entities, commands, events, errors
- Application layer with CommandHandler
- Domain ports for infrastructure separation
- Architecture tests with ArchUnit
- Complete documentation

Framework ready for company-wide adoption.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Implementation Complete!

This plan implements the full domain-centric architecture framework using the User bounded context as the reference. Each task follows TDD principles with failing tests first, minimal implementation, and immediate commits.

**Next Steps:**
1. Implement infrastructure adapters (PostgresEventStore, UserRepositoryImpl)
2. Create REST controller for CreateUser endpoint
3. Add integration tests with TestContainers
4. Create additional bounded contexts following the User pattern
5. Extract common patterns into company-wide template

**Skills Referenced:**
- @superpowers:executing-plans for task-by-task execution
