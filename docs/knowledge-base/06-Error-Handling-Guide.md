# Error Handling Guide

Comprehensive guide to error handling using Railway Oriented Programming (ROP).

## Philosophy

**No exceptions for business logic.** All errors are explicit, typed, and handled through the `Result<T, E>` type.

### Why ROP Over Exceptions?

| Aspect | Exceptions | Result Type |
|--------|-----------|-------------|
| Visibility | Hidden in signature | Explicit in type |
| Compiler help | Optional | Forced handling |
| Chaining | Try-catch nesting | Flat composition |
| Testing | Hard to assert | Easy to assert |
| Type safety | Runtime only | Compile-time |

## Result Type Basics

### Creating Results

```java
// Success
Result<User, UserError> user = Result.success(user);

// Failure
Result<User, UserError> error = Result.failure(USER_NOT_FOUND);

// From value object
Result<EmailAddress, String> email = EmailAddress.of("test@example.com");
```

### Checking Results

```java
if (result.isSuccess()) {
    User user = result.get();
    // handle success
}

if (result.isFailure()) {
    UserError error = result.getError();
    // handle error
}
```

### Transforming Results

```java
// Map: Transform success value
Result<String, UserError> name = result.map(user -> user.name().value());

// FlatMap: Chain Results
Result<User, UserError> loaded = userId
    .flatMap(id -> repository.findById(id));

// Peek: Side effects
result.peek(user -> log.info("Loaded: {}", user))
      .peekError(error -> log.error("Failed: {}", error.code()));
```

## Error Taxonomy

### Error Types

```
<Domain>Error (sealed interface)
├── ValidationError (HTTP 400)
│   └── Invalid input format/structure
├── ConflictError (HTTP 409)
│   └── Resource already exists or version conflict
├── PreConditionError (HTTP 422)
│   └── Business rule violation (state-based)
├── DomainError (HTTP 422)
│   └── Business rule violation (logic-based)
├── ConcurrencyError (HTTP 409)
│   └── Optimistic locking failure
└── PersistenceError (HTTP 500)
    └── Database/system failure
```

### Defining Errors

```java
public sealed interface UserError permits
        UserError.ValidationError,
        UserError.ConflictError,
        UserError.PreConditionError {

    String code();
    String message();
    Map<String, Object> metadata();

    // Validation errors - bad input
    public record ValidationError(
        String code,
        String message,
        Map<String, Object> metadata
    ) implements UserError {

        public static final ValidationError INVALID_EMAIL_FORMAT =
            new ValidationError(
                "INVALID_EMAIL_FORMAT",
                "Email address format is invalid",
                Map.of("field", "email", "pattern", "^[^@]+@[^@]+$")
            );
    }

    // Conflict errors - resource/state conflicts
    public record ConflictError(
        String code,
        String message,
        Map<String, Object> metadata
    ) implements UserError {

        public static final ConflictError EMAIL_ALREADY_IN_USE =
            new ConflictError(
                "EMAIL_ALREADY_IN_USE",
                "An account with this email already exists",
                Map.of("resource", "user", "field", "email")
            );
    }

    // Pre-condition errors - state-based violations
    public record PreConditionError(
        String code,
        String message,
        Map<String, Object> metadata
    ) implements UserError {

        public static final PreConditionError USER_ALREADY_SUSPENDED =
            new PreConditionError(
                "USER_ALREADY_SUSPENDED",
                "User is already suspended",
                Map.of("field", "status", "current", "SUSPENDED")
            );
    }
}
```

## Error Handling Patterns

### Pattern 1: Validation Chain

```java
public Result<ValidatedUser, UserError> validate(CreateUserRequest request) {
    return EmailAddress.of(request.email())
        .mapError(msg -> INVALID_EMAIL_FORMAT)
        .combine(
            UserName.of(request.name()),
            (email, name) -> new ValidatedUser(email, name)
        )
        .combine(
            PasswordHash.validate(request.password()),
            (user, hash) -> user.withPassword(hash)
        );
}
```

### Pattern 2: Early Return

```java
public Result<User, UserError> process(Command command) {
    // Check pre-condition
    if (status == SUSPENDED) {
        return Result.failure(USER_ALREADY_SUSPENDED);
    }

    // Execute logic
    return Result.success(new Event(...));
}
```

### Pattern 3: Error Recovery

```java
public Result<User, UserError> findOrCreate(String email) {
    return findByEmail(email)
        .recover(error -> {
            if (error == USER_NOT_FOUND) {
                return createDefaultUser(email);
            }
            throw new UnrecoverableError(error);
        });
}
```

### Pattern 4: Error Transformation

```java
// Map error to domain error
Result<EmailAddress, UserError> email = EmailAddress.of(input)
    .mapError(msg -> UserError.ValidationError.of("email", "INVALID_FORMAT", msg));
```

### Pattern 5: Fold to Single Value

```java
Response response = result.fold(
    user -> Response.ok(UserDTO.from(user)).build(),
    error -> ErrorResponseMapper.toResponse(error)
);
```

## Error to HTTP Mapping

### Mapping Function

```java
public class ErrorResponseMapper {

    public static Response toResponse(UserError error) {
        return switch (error) {
            case ValidationError e -> Response
                .status(Response.Status.BAD_REQUEST)  // 400
                .entity(ErrorDTO.from(error))
                .build();

            case ConflictError e -> Response
                .status(Response.Status.CONFLICT)  // 409
                .header("Retry-After", "1")
                .entity(ErrorDTO.from(error))
                .build();

            case PreConditionError e -> Response
                .status(422)  // Unprocessable Entity
                .entity(ErrorDTO.from(error))
                .build();

            case ConcurrencyError e -> Response
                .status(Response.Status.CONFLICT)  // 409
                .header("Retry-After", "1")
                .entity(ErrorDTO.from(error))
                .build();

            case PersistenceError e -> Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)  // 500
                .entity(ErrorDTO.from(error))
                .build();
        };
    }
}
```

### Error DTO

```java
public record ErrorDTO(
    int status,
    String code,
    String message,
    Map<String, Object> metadata,
    Instant timestamp
) {
    public static ErrorDTO from(UserError error) {
        return new ErrorDTO(
            statusCodeFor(error),
            error.code(),
            error.message(),
            error.metadata(),
            Instant.now()
        );
    }
}
```

## Testing Error Cases

### Unit Test Example

```java
@Test
@DisplayName("Suspend already suspended user should return error")
void suspendSuspendedUserShouldReturnError() {
    // Given
    var user = new User(id, email, name, roles, SUSPENDED);
    var command = new SuspendUser(id);

    // When
    var result = user.process(command);

    // Then
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError())
        .isEqualTo(PreConditionError.USER_ALREADY_SUSPENDED);

    // Optionally assert on error details
    assertThat(result.getError().code())
        .isEqualTo("USER_ALREADY_SUSPENDED");
    assertThat(result.getError().message())
        .isNotBlank();
}
```

### Integration Test Example

```java
@Test
@DisplayName("POST /users with duplicate email should return 409")
void postUsersWithDuplicateEmailShouldReturn409() {
    // Given
    var request = new CreateUserRequest("existing@example.com", "pass", "name");
    when(repository.emailExists(any())).thenReturn(true);

    // When & Then
    given()
        .contentType(JSON)
        .body(request)
        .post("/users")
        .then()
        .statusCode(409)
        .body("code", equalTo("EMAIL_ALREADY_IN_USE"))
        .body("message", notNullValue());
}
```

## Best Practices

### ✅ DO

1. **Use specific error types**
```java
return Result.failure(USER_ALREADY_SUSPENDED);  // Good
```

2. **Include helpful metadata**
```java
Map.of("field", "email", "minLength", 12, "actualLength", 5)
```

3. **Handle all errors explicitly**
```java
result.fold(
    success -> handleSuccess(success),
    error -> handleError(error)
);
```

4. **Create error constants**
```java
public static final ValidationError INVALID_EMAIL_FORMAT = ...
```

5. **Chain validation**
```java
emailResult
    .combine(nameResult, combiner)
    .combine(passwordResult, combiner);
```

### ❌ DON'T

1. **Don't use exceptions for business logic**
```java
// WRONG
if (invalid) throw new ValidationException();
```

2. **Don't ignore errors**
```java
// WRONG
if (result.isSuccess()) {
    // handle success
}
// Forgot to handle failure!
```

3. **Don't lose error context**
```java
// WRONG
return Result.failure("Error");  // What error?
```

4. **Don't return null on error**
```java
// WRONG
public User findUser(String id) {
    if (notFound) return null;
    return user;
}
```

5. **Don't mix exceptions with Result**
```java
// WRONG
try {
    return Result.success(operation());
} catch (Exception e) {
    return Result.failure(errorFrom(e));
}
```

## Error Handling in Different Layers

### Domain Layer

```java
// Return Result<Event, Error>
public Result<UserEvents, UserError> process(Command command) {
    if (invalid) {
        return Result.failure(DOMAIN_ERROR);
    }
    return Result.success(new Event(...));
}
```

### Application Layer

```java
// Chain Results, transform errors
public Result<ResponseDTO, UserError> execute(RequestDTO request) {
    return validate(request)
        .flatMap(this::checkPreConditions)
        .flatMap(this::executeCommand)
        .map(this::toResponseDTO);
}
```

### Infrastructure Layer

```java
// Map technical errors to domain errors
public Result<User, UserError> findById(UserId id) {
    try {
        var user = jooqSelect(...);
        return Result.success(user);
    } catch (DataAccessException e) {
        return Result.failure(PersistenceError.QUERY_FAILED);
    }
}
```

### REST Layer

```java
// Convert Result to Response
return service.execute(request)
    .fold(
        response -> Response.ok(response).build(),
        error -> ErrorResponseMapper.toResponse(error)
    );
```

## Advanced Patterns

### Pattern: Retry on Concurrency Error

```java
public Result<Response, Error> execute(Request request) {
    return executeWithRetry(request, MAX_RETRIES);
}

private Result<Response, Error> executeWithRetry(Request request, int retries) {
    var result = commandHandler.handle(...);

    if (result.isFailure() && result.getError() instanceof ConcurrencyError) {
        if (retries > 0) {
            // Reload and retry
            return executeWithRetry(request, retries - 1);
        }
    }

    return result;
}
```

### Pattern: Aggregate Errors

```java
public record AggregatedError(
    List<UserError> errors
) implements UserError {

    public String message() {
        return errors.stream()
            .map(UserError::message)
            .collect(Collectors.joining("; "));
    }
}

// Usage
var emailResult = EmailAddress.of(email);
var nameResult = UserName.of(name);

if (emailResult.isFailure() || nameResult.isFailure()) {
    var errors = new ArrayList<UserError>();
    if (emailResult.isFailure()) errors.add(from(emailResult.getError()));
    if (nameResult.isFailure()) errors.add(from(nameResult.getError()));
    return Result.failure(new AggregatedError(errors));
}
```

### Pattern: Error Logging

```java
public Result<T, E> logAndReturn(Result<T, E> result, String operation) {
    return result.peekError(error ->
        log.error("Operation {} failed: {} - {}",
            operation,
            error.code(),
            error.message())
    );
}
```

---

**Related:**
- [Domain Model Guide](./04-Domain-Model-Guide.md)
- [Quick Reference](./16-Quick-Reference.md)
- [Testing Strategies](./10-Testing-Strategies.md)
