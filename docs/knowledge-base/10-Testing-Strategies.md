# Testing Strategies

> **Note:** This guide uses the **User** bounded context as a concrete example to illustrate patterns.
> The concepts, principles, and code patterns apply universally to any bounded context.
> Replace `User` with your entity (e.g., `Product`, `Order`, `Course`, `Invoice`) and
> `UserId` with your ID type — the architecture remains the same.

Comprehensive guide to testing applications built with Domain-Centric Architecture.

## Testing Philosophy

- **Test first** (TDD): Write failing test, implement, watch it pass
- **Test behavior**: Test what the code does, not how it does it
- **Test isolated**: Unit tests should not touch external systems
- **Test comprehensively**: Aim for high coverage in domain layer

## Testing Pyramid

```
                    ┌─────────────┐
                    │   E2E Tests │  5% (Critical paths only)
                    └─────────────┘
                  ┌─────────────────┐
                  │  Integration    │  15% (Adapters, ports)
                  └─────────────────┘
               ┌────────────────────────┐
               │    Unit Tests          │  80% (Domain, Application)
               └────────────────────────┘
```

## Unit Tests

### Domain Entity Tests

Test entity behavior in complete isolation.

```java
class UserTest {

    @Test
    @DisplayName("Suspend active user should return UserSuspended event")
    void suspendActiveUserShouldReturnEvent() {
        // Given
        var user = new User(
            UserId.generate(),
            EmailAddress.of("test@example.com").get(),
            UserName.of("Test User").get(),
            Set.of(UserRole.STUDENT),
            UserStatus.ACTIVE
        );
        var command = new UserCommands.SuspendUser(user.userId());

        // When
        var result = user.process(command);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get())
            .isInstanceOf(UserEvents.UserSuspended.class);

        var event = (UserEvents.UserSuspended) result.get();
        assertThat(event.userId()).isEqualTo(user.userId());
        assertThat(event.version()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Suspend already suspended user should return error")
    void suspendAlreadySuspendedUserShouldReturnError() {
        // Given
        var user = new User(
            UserId.generate(),
            EmailAddress.of("test@example.com").get(),
            UserName.of("Test User").get(),
            Set.of(UserRole.STUDENT),
            UserStatus.SUSPENDED
        );
        var command = new UserCommands.SuspendUser(user.userId());

        // When
        var result = user.process(command);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError())
            .isEqualTo(UserError.PreConditionError.USER_ALREADY_SUSPENDED);
    }
}
```

### Value Object Tests

Test validation logic.

```java
class EmailAddressTest {

    @Test
    @DisplayName("Valid email should succeed")
    void validEmailShouldSucceed() {
        var result = EmailAddress.of("test@example.com");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().value()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Invalid email should fail with descriptive error")
    void invalidEmailShouldFail() {
        var result = EmailAddress.of("invalid");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Invalid email format");
    }

    @Test
    @DisplayName("Equal emails should be equal")
    void equalEmailsShouldBeEqual() {
        var email1 = EmailAddress.of("test@example.com").get();
        var email2 = EmailAddress.of("test@example.com").get();

        assertThat(email1).isEqualTo(email2);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("Blank email should fail")
    void blankEmailShouldFail(String value) {
        var result = EmailAddress.of(value);

        assertThat(result.isFailure()).isTrue();
    }
}
```

### Result Type Tests

Test ROP combinators.

```java
class ResultTest {

    @Test
    @DisplayName("Map should transform success value")
    void mapShouldTransformSuccess() {
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

        Result<String, String> chained = result.flatMap(i ->
            Result.success("x:" + i)
        );

        assertThat(chained.get()).isEqualTo("x:5");
    }

    @Test
    @DisplayName("Combine should combine two Results")
    void combineShouldCombineTwoResults() {
        Result<Integer, String> r1 = Result.success(5);
        Result<String, String> r2 = Result.success("test");

        Result<String, String> combined = r1.combine(r2, (i, s) -> s + ":" + i);

        assertThat(combined.get()).isEqualTo("test:5");
    }
}
```

### Factory Tests

Test creation logic.

```java
class UserFactoryTest {

    @Test
    @DisplayName("CreateUser with valid data should return UserCreated event")
    void createUserWithValidDataShouldReturnEvent() {
        var command = new UserCommands.CreateUser(
            "test@example.com",
            "hashedPassword",
            Set.of(UserRole.STUDENT),
            "Test User"
        );

        var result = UserFactory.createNewUser(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get())
            .isInstanceOf(UserEvents.UserCreated.class);

        var event = (UserEvents.UserCreated) result.get();
        assertThat(event.email().value()).isEqualTo("test@example.com");
        assertThat(event.status()).isEqualTo(UserStatus.PENDING_ACTIVATION);
    }

    @Test
    @DisplayName("CreateUser with invalid email should return error")
    void createUserWithInvalidEmailShouldReturnError() {
        var command = new UserCommands.CreateUser(
            "invalid-email",
            "hash",
            Set.of(UserRole.STUDENT),
            "Test"
        );

        var result = UserFactory.createNewUser(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError())
            .isEqualTo(UserError.ValidationError.INVALID_EMAIL_FORMAT);
    }
}
```

## Integration Tests

### Repository Tests

Test with TestContainers for real database.

```java
@Testcontainers
class PostgresUserRepositoryTest {

    @Container
    static final PostgreSQL<?> postgres = new PostgreSQL<>("postgres:16");

    private DSLContext jooq;
    private UserRepository repository;

    @BeforeEach
    void setUp() {
        jooq = DSLContextUsing JDBC.connect(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );
        repository = new PostgresUserRepository(jooq);
    }

    @Test
    @DisplayName("FindById should return user when exists")
    void findByIdShouldReturnUserWhenExists() {
        // Given
        var userId = UserId.generate();
        jooq.insertInto(USERS)
            .set(ID, userId.value())
            .set(EMAIL, "test@example.com")
            .execute();

        // When
        var result = repository.findById(userId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().email().value())
            .isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("FindById should return error when not exists")
    void findByIdShouldReturnErrorWhenNotExists() {
        // Given
        var userId = UserId.generate();

        // When
        var result = repository.findById(userId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError())
            .isEqualTo(UserError.NotFound.USER_NOT_FOUND);
    }
}
```

### REST Controller Tests

Test HTTP endpoints with Quarkus test.

```java
@QuarkusTest
class UserControllerTest {

    @InjectMock CreateUserUserService service;

    @Test
    @DisplayName("POST /users should return 201 on success")
    void postUsersShouldReturn201OnSuccess() {
        // Given
        var request = new CreateUserRequest(
            "test@example.com",
            "SecurePass123!",
            "John Doe"
        );
        var response = new UserResponseDTO(
            "user-123",
            "test@example.com",
            "John Doe",
            Set.of("STUDENT"),
            "PENDING_ACTIVATION"
        );

        when(service.execute(request))
            .thenReturn(Result.success(response));

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/users")
            .then()
            .statusCode(201)
            .body("userId", equalTo("user-123"))
            .body("email", equalTo("test@example.com"));
    }

    @Test
    @DisplayName("POST /users should return 400 on validation error")
    void postUsersShouldReturn400OnValidationError() {
        // Given
        var request = new CreateUserRequest("invalid", "pass", "name");
        var error = UserError.ValidationError.INVALID_EMAIL_FORMAT;

        when(service.execute(request))
            .thenReturn(Result.failure(error));

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/users")
            .then()
            .statusCode(400)
            .body("code", equalTo("INVALID_EMAIL_FORMAT"))
            .body("message", notNullValue());
    }
}
```

### Application Service Tests

Test orchestration with mocked ports.

```java
@ExtendWith(MockitoExtension.class)
class CreateUserUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock EventStore eventStore;
    @Mock ProjectionUpdater projectionUpdater;
    @Mock SideEffectTrigger sideEffectTrigger;
    @Mock CreateUserInput input;
    @Mock UserPreConditionChecker preConditionChecker;

    private CommandHandler<User, UserError> commandHandler;
    private CreateUserUserService service;

    @BeforeEach
    void setUp() {
        commandHandler = new CommandHandler<>(
            eventStore,
            projectionUpdater,
            sideEffectTrigger
        );
        service = new CreateUserUserService(
            commandHandler,
            input,
            preConditionChecker
        );
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        // Given
        var request = new CreateUserRequest("test@example.com", "pass", "name");
        var command = new UserCommands.CreateUser("test@example.com", "hash", Set.of(STUDENT), "name");
        var event = new UserEvents.UserCreated(...);

        when(input.process(request)).thenReturn(Result.success(command));
        when(preConditionChecker.checkEmailNotInUse(any())).thenReturn(Result.success(null));
        when(eventStore.append(event)).thenReturn(Result.success(new PersistedEvent(event, 1L, now())));
        when(projectionUpdater.update(any())).thenReturn(Result.success(null));

        // When
        var result = service.execute(request);

        // Then
        assertThat(result.isSuccess()).isTrue();

        verify(eventStore).append(event);
        verify(projectionUpdater).update(any());
        verify(sideEffectTrigger).triggerAsync(any());
    }

    @Test
    @DisplayName("Should return error when pre-condition fails")
    void shouldReturnErrorWhenPreConditionFails() {
        // Given
        var request = new CreateUserRequest("existing@example.com", "pass", "name");
        var command = mock(UserCommands.CreateUser.class);
        var conflictError = UserError.ConflictError.EMAIL_ALREADY_IN_USE;

        when(input.process(request)).thenReturn(Result.success(command));
        when(preConditionChecker.checkEmailNotInUse(any()))
            .thenReturn(Result.failure(conflictError));

        // When
        var result = service.execute(request);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo(conflictError);

        verifyNoInteractions(eventStore);
    }
}
```

## Architecture Tests

### ArchUnit Tests

Enforce dependency rules.

```java
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
                    "com.k12.common.domain.."
                );

    @ArchTest
    static final ArchRule entities_should_be_records =
        classes()
            .that().haveSimpleNameMatching(".*")
            .and().resideInAPackage("..domain.model")
            .and().areNotAssignableTo(Enum.class)
            .should().beRecords();

    @ArchTest
    static final ArchRule commands_should_be_sealed_interfaces =
        classes()
            .that().haveSimpleNameEndingWith("Commands")
            .should().beInterfaces()
            .andShould().beModifiers().withOnlySealedInterfaces();

    @ArchTest
    static final ArchRule no_exceptions_in_domain =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().throwAnyException();
}
```

## E2E Tests

### Full Flow Tests

Test critical user journeys from HTTP to database.

```java
@QuarkusTest
@Testcontainers
class CreateUserE2ETest {

    @Container
    static final PostgreSQL<?> postgres = new PostgreSQL<>("postgres:16");

    @Test
    @DisplayName("Should create user and return response")
    void shouldCreateUserAndReturnResponse() {
        // Given
        var request = """
            {
                "email": "test@example.com",
                "password": "SecurePass123!",
                "name": "Test User"
            }
            """;

        // When
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/users")
            .then()
            .statusCode(201)
            .body("email", equalTo("test@example.com"))
            .body("status", equalTo("PENDING_ACTIVATION"));

        // Then - verify in database
        var count = jooq.fetchCount(
            jooq.selectFrom(USER_PROJECTIONS)
                .where(USER_PROJECTIONS.EMAIL.eq("test@example.com"))
        );
        assertThat(count).isEqualTo(1);
    }
}
```

## Test Coverage Goals

### By Layer

| Layer | Coverage Target | Focus |
|-------|-----------------|-------|
| Domain | 100% | Business logic, invariants |
| Application | 90% | Orchestration, error handling |
| Infrastructure | 80% | Adapters, mappers |
| Driving/REST | 70% | HTTP endpoints, status codes |

### By Type

| Test Type | Coverage | Example |
|-----------|----------|---------|
| Happy path | All flows | Success scenarios |
| Error cases | All errors | Each error branch |
| Edge cases | Critical | Empty, null, boundary |
| Integration | Critical flows | Database, external APIs |

## Best Practices

### ✅ DO

1. **Use descriptive test names**
```java
@Test
@DisplayName("Suspend active user should return UserSuspended event")
void suspendActiveUserShouldReturnEvent() { }
```

2. **Follow AAA pattern** (Arrange-Act-Assert)
```java
// Arrange
var user = createUser();
var command = new SuspendUser(user.id());

// Act
var result = user.process(command);

// Assert
assertThat(result.isSuccess()).isTrue();
```

3. **Test one thing per test**
```java
// Good - one assertion
@Test
void shouldReturnErrorWhenAlreadySuspended() {
    assertThat(result.getError()).isEqualTo(USER_ALREADY_SUSPENDED);
}

// Avoid - too many assertions
@Test
void testEverything() { }
```

4. **Use test builders**
```java
var user = TestData.user()
    .withId(id)
    .withEmail("test@example.com")
    .build();
```

5. **Mock at boundaries only**
```java
// Good - mock ports
@Mock UserRepository userRepository;

// Bad - mock entities
```

### ❌ DON'T

1. **Don't test implementation details**
```java
// Wrong
@Test
void testProcessMethodCallsSwitch() { }

// Right
@Test
void suspendCommandReturnsSuspensionEvent() { }
```

2. **Don't add unnecessary assertions**
```java
// Wrong - asserting on implementation
verify(user).process(command);
verify(eventStore).append(event);

// Right - assert on outcome
assertThat(result.isSuccess()).isTrue();
```

3. **Don't share state between tests**
```java
// Wrong
static User sharedUser;

@BeforeAll
static void setUp() { sharedUser = new User(); }
```

4. **Don't catch exceptions in tests**
```java
// Wrong
try {
    result = operation();
} catch (Exception e) {
    fail();
}

// Right
assertThat(result.isSuccess()).isTrue();
```

5. **Don't test getters/setters**
```java
// Wrong - records generate these
@Test
void testGetEmail() {
    assertThat(user.email()).isEqualTo(email);
}
```

## Test Utilities

### Test Data Builder

```java
public class TestData {

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private UserId id = UserId.generate();
        private EmailAddress email = EmailAddress.of("test@example.com").get();
        private UserName name = UserName.of("Test User").get();
        private Set<UserRole> roles = Set.of(UserRole.STUDENT);
        private UserStatus status = UserStatus.ACTIVE;

        public UserBuilder withId(UserId id) {
            this.id = id;
            return this;
        }

        public UserBuilder withEmail(String email) {
            this.email = EmailAddress.of(email).get();
            return this;
        }

        public User build() {
            return new User(id, email, name, roles, status);
        }
    }
}
```

### Custom Assertions

```java
public class ResultAssertions {

    public static <T, E> void assertSuccess(Result<T, E> result) {
        assertThat(result)
            .withFailMessage("Expected success but was failure: %s",
                result.getError().message())
            .isSuccess();
    }

    public static <T, E> void assertFailure(Result<T, E> result) {
        assertThat(result)
            .withFailMessage("Expected failure but was success")
            .isFailure();
    }
}

// Usage
assertThat(result).isSuccess();
ResultAssertions.assertSuccess(result);
```

---

**Related:**
- [Domain Model Guide](./04-Domain-Model-Guide.md)
- [Error Handling Guide](./06-Error-Handling-Guide.md)
- [Quick Reference](./16-Quick-Reference.md)
