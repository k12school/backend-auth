# Common Tasks

Step-by-step procedures for common development tasks.

## Task: Create a New Entity

### When to Use
Creating a new aggregate root in your bounded context.

### Steps

1. **Define the entity record**

```java
package com.k12.<context>.domain.model;

public record Product(
    ProductId productId,
    ProductName name,
    Money price,
    ProductStatus status
) {
    public Result<ProductEvents, ProductError> process(ProductCommands command) {
        return switch (command) {
            case CreateProduct c -> process(c);
            case DiscontinueProduct c -> process(c);
            case ChangePrice c -> process(c);
        };
    }
}
```

2. **Create commands interface**

```java
package com.k12.<context>.domain.model.commands;

public sealed interface ProductCommands {
    record CreateProduct(String name, Money price) implements ProductCommands {}
    record DiscontinueProduct(ProductId productId) implements ProductCommands {}
    record ChangePrice(ProductId productId, Money newPrice) implements ProductCommands {}
}
```

3. **Create events interface**

```java
package com.k12.<context>.domain.model.events;

public sealed interface ProductEvents {
    record ProductCreated(
        ProductId productId,
        ProductName name,
        Money price,
        Instant createdAt,
        long version
    ) implements ProductEvents {}

    record ProductDiscontinued(
        ProductId productId,
        Instant discontinuedAt,
        long version
    ) implements ProductEvents {}
}
```

4. **Create errors interface**

```java
package com.k12.<context>.domain.model.error;

public sealed interface ProductError {
    record ValidationError(String code, String message, Map<String, Object> metadata) implements ProductError {
        public static final ValidationError INVALID_PRICE = new ValidationError(
            "INVALID_PRICE", "Price must be positive", Map.of()
        );
    }
    // ... other error types
}
```

5. **Write tests**

```java
class ProductTest {
    @Test
    void discontinueActiveProductShouldReturnEvent() {
        var product = new Product(id, name, price, ACTIVE);
        var result = product.process(new DiscontinueProduct(id));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isInstanceOf(ProductDiscontinued.class);
    }
}
```

---

## Task: Add a New Command to Existing Entity

### When to Use
Adding new behavior to an existing aggregate.

### Steps

1. **Add command record**

```java
public sealed interface ProductCommands {
    // ... existing commands
    record RestockProduct(ProductId productId, int quantity) implements ProductCommands {}
}
```

2. **Add event record**

```java
public sealed interface ProductEvents {
    // ... existing events
    record ProductRestocked(
        ProductId productId,
        int quantityAdded,
        int newQuantity,
        Instant restockedAt,
        long version
    ) implements ProductEvents {}
}
```

3. **Add error type (if needed)**

```java
public sealed interface ProductError {
    // ... existing errors
    record DomainError(String code, String message, Map<String, Object> metadata) implements ProductError {
        public static final DomainError NEGATIVE_QUANTITY = new DomainError(
            "NEGATIVE_QUANTITY",
            "Quantity cannot be negative",
            Map.of("minQuantity", 0)
        );
    }
}
```

4. **Implement handler in entity**

```java
public record Product(...) {
    public Result<ProductEvents, ProductError> process(ProductCommands command) {
        return switch (command) {
            // ... existing cases
            case RestockProduct c -> process(c);
        };
    }

    private Result<ProductEvents, ProductError> process(RestockProduct command) {
        if (command.quantity() <= 0) {
            return Result.failure(DomainError.NEGATIVE_QUANTITY);
        }

        int newQuantity = this.quantity + command.quantity();
        return Result.success(new ProductRestocked(
            productId, command.quantity(), newQuantity, now(), nextVersion()
        ));
    }
}
```

5. **Add event application in Reconstructor**

```java
public static Product applyEvent(Product product, ProductEvents event) {
    return switch (event) {
        // ... existing cases
        case ProductRestocked e -> new Product(
            product.productId(),
            product.name(),
            product.price(),
            e.newQuantity(),
            product.status()
        );
    };
}
```

6. **Write tests**

```java
@Test
void restockWithPositiveQuantityShouldReturnEvent() {
    var product = new Product(id, name, price, 100, ACTIVE);
    var result = product.process(new RestockProduct(id, 50));

    assertThat(result.isSuccess()).isTrue();
    var event = (ProductRestocked) result.get();
    assertThat(event.newQuantity()).isEqualTo(150);
}

@Test
void restockWithNegativeQuantityShouldReturnError() {
    var product = new Product(id, name, price, 100, ACTIVE);
    var result = product.process(new RestockProduct(id, -10));

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError()).isEqualTo(DomainError.NEGATIVE_QUANTITY);
}
```

---

## Task: Create a REST Endpoint

### When to Use
Exposing entity operations via HTTP.

### Steps

1. **Create request DTO**

```java
package com.k12.<context>.infrastructure.driving.rest;

public class SuspendUserRequestDTO {
    @NotNull
    private String userId;
    // getters, setters
}
```

2. **Create response DTO**

```java
public class UserResponseDTO {
    private String userId;
    private String email;
    private String status;
    // getters, setters

    public static UserResponseDTO from(UserEvents.UserSuspended event) {
        var dto = new UserResponseDTO();
        dto.setUserId(event.userId().value());
        dto.setStatus("SUSPENDED");
        return dto;
    }
}
```

3. **Create error DTO**

```java
public class ErrorDTO {
    private int status;
    private String code;
    private String message;
    private Map<String, Object> metadata;
    private Instant timestamp;
    // getters, setters

    public static ErrorDTO from(UserError error) {
        var dto = new ErrorDTO();
        dto.setCode(error.code());
        dto.setMessage(error.message());
        dto.setMetadata(error.metadata());
        dto.setTimestamp(Instant.now());
        dto.setStatus(statusCodeFor(error));
        return dto;
    }

    private static int statusCodeFor(UserError error) {
        return switch (error) {
            case ValidationError e -> 400;
            case ConflictError e -> 409;
            case PreConditionError e -> 422;
            case DomainError e -> 422;
            case ConcurrencyError e -> 409;
            case PersistenceError e -> 500;
        };
    }
}
```

4. **Create controller**

```java
package com.k12.<context>.infrastructure.driving.rest;

import jakarta.ws.rs.*;
import jakarta.validation.Valid;

@Path("/users")
@Produces("application/json")
@Consumes("application/json")
public class UserController {

    private final SuspendUserService suspendUserService;

    @POST
    @Path("/{id}/suspend")
    public Response suspendUser(
            @PathParam("id") String id,
            @Valid SuspendUserRequestDTO request) {

        return suspendUserService.execute(request)
            .fold(
                success -> Response.ok(success).build(),
                error -> Response
                    .status(ErrorDTO.statusCodeFor(error))
                    .entity(ErrorDTO.from(error))
                    .build()
            );
    }
}
```

5. **Write integration test**

```java
@QuarkusTest
class UserControllerTest {

    @InjectMock SuspendUserService service;

    @Test
    void suspendUserShouldReturn200() {
        var request = new SuspendUserRequestDTO("user-123");
        var response = UserResponseDTO.from(new UserSuspended(...));

        when(service.execute(request)).thenReturn(Result.success(response));

        given()
            .contentType(JSON)
            .body(request)
            .post("/users/user-123/suspend")
            .then()
            .statusCode(200)
            .body("status", equalTo("SUSPENDED"));
    }
}
```

---

## Task: Add Validation to a Value Object

### When to Use
Enforcing business rules on value objects.

### Steps

1. **Define validation rules**

```java
public record Password(String value) {

    // Validation rules
    private static final int MIN_LENGTH = 12;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");

    public static Result<Password, String> of(String value) {
        List<String> errors = new ArrayList<>();

        if (value == null || value.isBlank()) {
            return Result.failure("Password cannot be null or blank");
        }
        if (value.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (!HAS_UPPERCASE.matcher(value).find()) {
            errors.add("Password must contain uppercase letter");
        }
        if (!HAS_LOWERCASE.matcher(value).find()) {
            errors.add("Password must contain lowercase letter");
        }
        if (!HAS_DIGIT.matcher(value).find()) {
            errors.add("Password must contain digit");
        }

        if (!errors.isEmpty()) {
            return Result.failure(String.join(", ", errors));
        }

        return Result.success(new Password(value));
    }

    // Hash the password
    public PasswordHash hash() {
        return PasswordHash.hash(this.value());
    }
}
```

2. **Use in entity**

```java
public record User(...) {
    public Result<UserEvents, UserError> process(ChangePassword command) {
        // Validate new password
        var passwordResult = Password.of(command.newPassword());
        if (passwordResult.isFailure()) {
            return Result.failure(new PasswordError(
                "INVALID_PASSWORD",
                passwordResult.getError(),
                Map.of()
            ));
        }

        // Check it's different from current
        if (passwordHash.value().equals(passwordResult.get().hash().value())) {
            return Result.failure(PASSWORD_SAME_AS_CURRENT);
        }

        return Result.success(new PasswordChanged(
            userId, passwordResult.get().hash(), now(), nextVersion()
        ));
    }
}
```

---

## Task: Implement a Repository Port

### When to Use
Creating infrastructure implementation of a domain port.

### Steps

1. **Define port in domain**

```java
package com.k12.<context>.domain.port.input;

public interface ProductRepository {
    Result<Product, ProductError> findById(ProductId id);
    boolean exists(ProductName name);
}
```

2. **Create implementation in infrastructure**

```java
package com.k12.<context>.infrastructure.driven.persistence;

@ApplicationScoped
public class PostgresProductRepository implements ProductRepository {

    private final DSLContext jooq;

    @Override
    public Result<Product, ProductError> findById(ProductId id) {
        try {
            var record = jooq.selectFrom(PRODUCTS)
                .where(PRODUCTS.ID.eq(id.value()))
                .fetchOne();

            if (record == null) {
                return Result.failure(ProductError.NotFound.PRODUCT_NOT_FOUND);
            }

            var product = new Product(
                ProductId.of(record.getId()),
                ProductName.of(record.getName()).get(),
                Money.of(record.getPrice()).get(),
                ProductStatus.valueOf(record.getStatus())
            );

            return Result.success(product);

        } catch (Exception e) {
            return Result.failure(ProductError.PersistenceError.QUERY_FAILED);
        }
    }

    @Override
    public boolean exists(ProductName name) {
        var count = jooq.fetchCount(
            jooq.selectFrom(PRODUCTS)
                .where(PRODUCTS.NAME.eq(name.value()))
        );
        return count > 0;
    }
}
```

3. **Test with TestContainers**

```java
@Testcontainers
class PostgresProductRepositoryTest {

    @Container
    static final PostgreSQL<?> postgres = new PostgreSQL<>("postgres:16");

    @Test
    void findByIdShouldReturnProduct() {
        var repo = new PostgresProductRepository(jooq);
        var id = ProductId.generate();

        // Insert test data
        jooq.insertInto(PRODUCTS)
            .set(PRODUCTS.ID, id.value())
            .set(PRODUCTS.NAME, "Test Product")
            .execute();

        // Test
        var result = repo.findById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().name().value()).isEqualTo("Test Product");
    }
}
```

---

## Task: Add a Pre-Condition Check

### When to Use
Validating conditions before loading an entity.

### Steps

1. **Create checker class**

```java
package com.k12.<context>.application.input.precondition;

@ApplicationScoped
public class UserPreConditionChecker {

    private final UserRepository userRepository;

    public Result<Void, UserError> checkEmailNotInUse(EmailAddress email) {
        if (userRepository.emailExists(email)) {
            return Result.failure(ConflictError.EMAIL_ALREADY_IN_USE);
        }
        return Result.success(null);
    }

    public Result<Void, UserError> checkTenantIsActive(String tenantId) {
        if (!userRepository.tenantIsActive(tenantId)) {
            return Result.failure(PreConditionError.TENANT_NOT_ACTIVE);
        }
        return Result.success(null);
    }
}
```

2. **Use in application service**

```java
public Result<UserResponseDTO, UserError> execute(CreateUserRequest request) {
    // Validate DTO
    var commandResult = input.process(request);
    if (commandResult.isFailure()) return Result.failure(commandResult.getError());

    // Check pre-conditions
    var checkResult = preConditionChecker.checkEmailNotInUse(
        EmailAddress.of(request.email()).get()
    );
    if (checkResult.isFailure()) return Result.failure(checkResult.getError());

    // Execute command
    return commandHandler.handle(loader, processor, mapper);
}
```

---

## Task: Create Architecture Tests

### When to Use
Enforcing dependency rules with ArchUnit.

### Steps

1. **Add ArchUnit dependency** (already in build.gradle.kts)

2. **Create test class**

```java
package com.k12.<context>.architecture;

@AnalyzeClasses(packages = "com.k12.<context>")
class <Context>ArchitectureTest {

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
}
```

3. **Run tests**

```bash
./gradlew test --tests "*ArchitectureTest"
```

---

## Task: Add Metrics Collection

### When to Use
Tracking domain command execution metrics.

### Steps

1. **Create metrics class**

```java
package com.k12.shared.infrastructure.metrics;

@ApplicationScoped
public class DomainMetrics {

    private final MeterRegistry registry;
    private final Counter commandSuccess;
    private final Counter commandFailure;

    public DomainMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.commandSuccess = Counter.builder("domain.command.success")
            .tag("context", "user")
            .register(registry);
        this.commandFailure = Counter.builder("domain.command.failure")
            .tag("context", "user")
            .register(registry);
    }

    public <T, E> Result<T, E> track(String commandType, Result<T, E> result) {
        if (result.isSuccess()) {
            commandSuccess.increment();
        } else {
            commandFailure.increment(Tags.of("command", commandType));
        }
        return result;
    }
}
```

2. **Wrap command execution**

```java
public Result<UserResponseDTO, UserError> execute(Request request) {
    return metrics.track("CreateUser",
        commandHandler.handle(loader, processor, mapper)
    );
}
```

3. **View metrics**

Exposes Prometheus metrics at `/q/metrics` endpoint.

---

## Task: Handle Version Conflicts

### When to Use
Optimistic locking with Event Sourcing.

### Steps

1. **Add unique constraint in database**

```sql
ALTER TABLE user_events
ADD CONSTRAINT user_events_version_key
UNIQUE (aggregate_id, version);
```

2. **Handle in EventStore**

```java
public Result<PersistedEvent, UserError> append(UserEvents event) {
    try {
        var record = jooq.insertInto(EVENTS)
            .set(AGGREGATE_ID, event.userId().value())
            .set(VERSION, event.version())
            .set(EVENT_DATA, JSON.valueOf(serialize(event)))
            .execute();

        return Result.success(new PersistedEvent(event, event.version(), now()));

    } catch (DataIntegrityViolationException e) {
        // Unique constraint violation = version conflict
        return Result.failure(ConcurrencyError.VERSION_CONFLICT);
    }
}
```

3. **Add retry logic in service**

```java
public Result<Response, Error> execute(Request request) {
    return executeWithRetry(request, MAX_RETRIES);
}

private Result<Response, Error> executeWithRetry(Request request, int retries) {
    var result = commandHandler.handle(loader, processor, mapper);

    if (result.isFailure() && result.getError() instanceof ConcurrencyError) {
        if (retries > 0) {
            // Reload entity and retry
            return executeWithRetry(request, retries - 1);
        }
    }

    return result;
}
```

---

## Need Another Task?

Submit a task to be added to this knowledge base through:
- Team Slack/Teams
- Architecture review meeting
- Pull request to this document

---

**Related:**
- [Quick Reference](./16-Quick-Reference.md)
- [Troubleshooting](./18-Troubleshooting.md)
- [FAQ](./19-FAQ.md)
