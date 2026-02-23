# Optimistic Locking Implementation for Tenant Aggregate

## Overview

The Tenant aggregate uses event sourcing with optimistic locking to ensure consistency
when multiple writers attempt to modify the same aggregate concurrently.

## Version Semantics

We use **forward-looking version semantics**:

- The caller provides the **expected NEW version number** when appending an event
- This represents the version that will be assigned to the event being appended
- After successfully appending version N, the next event must specify version N+1

**Example sequence:**
1. Create tenant → append event with version=1
2. Update tenant → append event with version=2
3. Update again → append event with version=3

This is different from "check current version, then increment" semantics, which would
require a read-before-write and introduce a race condition.

## Database Constraints

The `tenant_events` table has a unique constraint on `(tenant_id, version)`:

```sql
ALTER TABLE tenant_events 
ADD CONSTRAINT uq_tenant_event_version 
UNIQUE (tenant_id, version);
```

This constraint ensures:
- Each tenant can have only one event with a given version number
- The database enforces uniqueness atomically at the transaction level

## Conflict Detection

### How It Works

1. When appending an event, we use `INSERT ... ON CONFLICT ... DO NOTHING`:
   ```java
   int inserted = ctx.insertInto(TENANT_EVENTS, ...)
       .values(tenantId, eventType, eventData, expectedVersion, ...)
       .onConflict(TENANT_EVENTS.TENANT_ID, TENANT_EVENTS.VERSION)
       .doNothing()
       .execute();
   ```

2. If `inserted == 0`, the event already exists (version conflict):
   ```java
   if (inserted == 0) {
       return Result.failure(VERSION_CONFLICT);
   }
   ```

3. If `inserted == 1`, the event was appended successfully

### Why This Is Correct

- **No race condition**: The database constraint check happens atomically during insert
- **No check-then-act**: We don't need to read the current version before writing
- **Automatic retry**: Clients can catch VERSION_CONFLICT, reload events, and retry with the correct version

### Mapping to Domain Error

```java
private static final TenantError VERSION_CONFLICT = 
    TenantError.ConcurrencyError.VERSION_CONFLICT;
```

When the unique constraint is violated (inserted=0), we return this error to the caller,
who can then:
1. Load the current event stream to get the latest version
2. Apply their command on the latest state
3. Retry with the correct new version number

## Atomicity Guarantee

The `@Transactional` annotation ensures that event append and projection update
happen atomically:

```java
@Override
@Transactional
public Result<Void, TenantError> append(TenantEvents event, long expectedVersion) {
    // 1. Insert event (with conflict detection)
    int inserted = ctx.insertInto(...).onConflict(...).doNothing().execute();
    
    // 2. Update projection (only if event was inserted)
    if (inserted == 1) {
        updateProjection(ctx, event);
    }
    
    return Result.success(null);
}
```

If the event insert fails due to conflict, the projection is not updated.
If the projection update fails, the entire transaction (including event insert) is rolled back.

## Integration Tests

See `TenantRepositoryIntegrationTest` for demonstrations of:
- Concurrent append conflict detection
- Sequential version enforcement
- Atomicity guarantees
- Projection rollback on conflict

## References

- jOOQ `INSERT ... ON CONFLICT`: https://www.jooq.org/doc/latest/manual/sql-building/sql-statements/insert-statement/insert-on-conflict/
- PostgreSQL Unique Constraints: https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-UNIQUE-CONSTRAINTS
- Optimistic Locking Pattern: https://martinfowler.com/eaaCatalog/optimisticOfflineLock.html
