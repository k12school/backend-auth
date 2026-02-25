# Performance Profiling Audit Report

**Generated:** 2026-02-25
**Purpose:** Audit available monitoring capabilities for performance profiling

---

## Executive Summary

You have **partial monitoring coverage** for performance profiling. Some metrics are available out-of-the-box, but critical gaps exist for:
- ❌ Database query timing (JDBC metrics)
- ❌ Serialization/deserialization timing
- ❌ Application-level custom metrics

---

## What's Currently Available ✅

### 1. HTTP Server Request Metrics
**Endpoint:** `http://localhost:8080/metrics`

**Available Metrics:**
```
http_server_requests_seconds{method, outcome, status, uri}
  - Request timing for each endpoint
  - Examples:
    * POST /api/tenants (201): 109ms
    * POST /api/auth/login (200): 1533ms
    * POST /api/tenants/{id}/activate (200): 19ms
```

**What it tells you:**
- ✅ Which endpoints are slow
- ✅ Response time distribution (P50, P95, P99)
- ✅ Error rates by endpoint
- ⚠️ **Missing:** Breakdown of where time is spent (DB vs. app vs. serialization)

### 2. Worker Pool Metrics
```
worker_pool_usage_seconds{pool_name, pool_type}
worker_pool_queue_delay_seconds{pool_name, pool_type}
worker_pool_active{pool_name, pool_type}
worker_pool_rejected{pool_name, pool_type}
```

**What it tells you:**
- ✅ Thread pool utilization
- ✅ Task queue backlog
- ✅ Rejected tasks (backpressure)
- ⚠️ **Missing:** Per-endpoint breakdown

### 3. JVM Metrics
```
jvm_gc_pause_seconds
jvm_memory_used_bytes{area, id}
jvm_threads_live_threads
```

**What it tells you:**
- ✅ GC overhead and frequency
- ✅ Heap vs. off-heap memory usage
- ✅ Thread count
- ⚠️ **Missing:** Per-request allocation rates

### 4. Process & System Metrics
```
process_cpu_usage
system_cpu_usage
process_files_open_files
```

**What it tells you:**
- ✅ CPU utilization
- ✅ File descriptor usage
- ⚠️ **Missing:** I/O wait, context switches

### 5. Container Metrics (cAdvisor)
**Endpoint:** `http://localhost:8081/metrics`

```
container_cpu_usage_seconds_total{id, cpu}
container_memory_usage_bytes{id}
container_fs_reads_bytes_total{id}
```

**What it tells you:**
- ✅ Per-container CPU/memory
- ✅ Disk I/O per container
- ⚠️ **Missing:** Network I/O breakdown per service

### 6. Host Metrics (node-exporter)
**Endpoint:** `http://localhost:9100/metrics`

```
node_cpu_seconds_total{cpu, mode}
node_memory_MemAvailable_bytes
node_load_average_1m
```

**What it tells you:**
- ✅ System-wide CPU/memory/disk
- ⚠️ **Missing:** Per-process breakdown

---

## What's Missing ❌

### 1. Database Query Timing (CRITICAL)
**Status:** ⚠️ Configured but not producing metrics

**Problem:** `quarkus.micrometer.binder.jdbc.enabled=true` is set, but no JDBC metrics appear

**Root Cause:** The property `quarkus.micrometer.binder.jdbc.enabled` is **not recognized** by Quarkus 3.31.2 (see warnings in logs)

**What you need:**
```properties
# Add Micrometer JDBC Timer aspect
quarkus.micrometer.binder.http-server.enabled=true  # ✅ Working
quarkus.micrometer.binder.jpa.enabled=true          # ❌ Try this instead
```

**Alternative:** Enable Quarkus Hibernate statistics:
```properties
quarkus.hibernate-statistics.enabled=true
quarkus.log.category."org.hibernate.stat".level=INFO
```

### 2. Serialization/Deserialization Timing
**Status:** ❌ Not available

**Why:** JSON serialization happens inside the HTTP server layer, but Quarkus doesn't expose separate metrics for:
- JSON parse time (request deserialization)
- JSON serialize time (response generation)

**Solution options:**
1. **Custom Micrometer timers** around ObjectMapper calls
2. **HTTP server metrics** include serialization (but not separated)
3. **OpenTelemetry spans** would show this (but traces aren't exporting)

### 3. Application-Level Business Metrics
**Status:** ❌ Not implemented

**Missing examples:**
- Time spent in business logic (excluding DB/serialization)
- Cache hit/miss rates
- External API call timings
- Queue processing times

**Solution:** Add custom Micrometer metrics:
```java
@Timed(value = "business.tenant.create", description = "Time to create tenant")
public Tenant createTenant(CreateTenantCommand cmd) {
    // business logic
}
```

### 4. PostgreSQL Database Metrics
**Status:** ❌ postgres-exporter defined but not running

**Container:** `k12-postgres-exporter` - NOT RUNNING

**What it would provide:**
- `pg_stat_database` - Query timing, commit/rollback rates
- `pg_stat_statements` - Per-query timing (requires extension)
- Table/index bloat, lock contention

**Fix:**
```bash
docker compose -f docker-compose.monitoring.yml up -d postgres-exporter
```

---

## How to Enable Missing Metrics

### Option 1: Enable Hibernate Statistics (Quick Win)

Add to `application.properties`:
```properties
quarkus.hibernate-statistics.enabled=true
quarkus.hibernate-statistics.logs-enabled=true
quarkus.log.category."org.hibernate.stat".level=DEBUG
```

**Output:** You'll see query execution times in logs:
```
[Hibernate] fetchSize=2, batchSize=10, lazy=true, resultSetSize=2
[Hibernate] query time: 15ms
```

### Option 2: Enable JPA Metrics (Micrometer)

Add to `application.properties`:
```properties
quarkus.micrometer.binder.jpa.enabled=true
```

**Metrics produced:**
```
jpa.cache.hit_ratio{region}
jpa.query.execution.time{query_name}
```

### Option 3: Add Custom Application Metrics

Add dependency to `build.gradle.kts`:
```kotlin
implementation("io.micrometer:micrometer-core")
```

Annotate methods:
```java
import io.micrometer.core.annotation.Timed;

@Timed(value = "app.tenant.create", percentiles = {0.5, 0.95, 0.99})
public Tenant createTenant(CreateTenantCommand cmd) {
    // business logic
}
```

**Metrics produced:**
```
app_tenant_create_seconds{quantile="0.50"}
app_tenant_create_seconds{quantile="0.95"}
app_tenant_create_seconds_count
```

### Option 4: Enable postgres-exporter

```bash
docker compose -f docker-compose.monitoring.yml up -d postgres-exporter
```

**Access metrics:**
```bash
curl http://localhost:9187/metrics | grep pg_stat
```

### Option 5: Add Request Context Timing

Add custom filter to track request phases:
```java
@Provider
@PreMatching
public class TimingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Record start time
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Calculate: deserialization, business logic, serialization phases
    }
}
```

---

## Recommended Immediate Actions

### Priority 1: Enable Database Query Timing (5 minutes)

**Option A:** Enable Hibernate statistics
```properties
quarkus.hibernate-statistics.enabled=true
quarkus.log.category."org.hibernate.stat".level=INFO
```

**Option B:** Enable postgres-exporter
```bash
docker compose -f docker-compose.monitoring.yml up -d postgres-exporter
curl http://localhost:9187/metrics | grep pg_stat_database
```

### Priority 2: Add Custom Business Logic Metrics (30 minutes)

1. Add `@Timed` annotations to key business methods
2. Create custom metrics for serialization timing
3. Verify metrics appear at `/metrics`

### Priority 3: Verify JDBC Metrics Work (if applicable)

Check if `quarkus.micrometer.binder.jpa.enabled=true` produces metrics in Quarkus 3.31.2.

---

## Testing Your Metrics

### 1. Generate Load
```bash
# 100 tenant creation requests
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/tenants \
    -H "Content-Type: application/json" \
    -d '{"name":"test'$i'","domain":"test'$i'.com"}'
done
```

### 2. Check Metrics
```bash
# HTTP timing
curl http://localhost:8080/metrics | grep http_server_requests_seconds

# If Hibernate stats enabled, check logs
tail -f log/k12-backend.log | grep "Hibernate statistics"

# If postgres-exporter running, check DB metrics
curl http://localhost:9187/metrics | grep pg_stat_database
```

### 3. Analyze
Look for:
- High variance in response times (indicates DB locks, GC pauses)
- P99 >> P50 (indicates outliers)
- Memory growth over time (indicates memory leak)

---

## Distributed Tracing Alternative (If It Gets Fixed)

If Quarkus → SigNoz trace export starts working, you'd get:

**Automatic spans for:**
- ✅ HTTP request (incoming)
- ✅ Database queries (JDBC)
- ✅ HTTP client calls (outgoing)
- ✅ Message queue operations
- ✅ Custom spans (manual)

**Span attributes:**
- `db.statement` - Exact SQL query
- `db.system` - "postgresql"
- `db.name` - "k12_db"
- `http.method`, `http.status_code` - Request details

**Time breakdown:**
```
HTTP Request (total: 150ms)
  ├─ Deserialization (20ms)
  ├─ Business Logic (60ms)
  │   └─ TenantService.create()
  ├─ Database Query (50ms)
  │   ├─ SELECT tenant (10ms)
  │   └─ INSERT tenant (40ms)
  └─ Serialization (20ms)
```

**Current status:** ❌ Traces created but not exported to SigNoz

---

## Summary Table

| Metric Category | Status | How to Enable |
|----------------|--------|---------------|
| HTTP Request Timing | ✅ Available | Already at `/metrics` |
| Database Query Timing | ❌ Missing | Enable `hibernate-statistics` OR `postgres-exporter` |
| Serialization Timing | ❌ Missing | Custom filter or OpenTelemetry (broken) |
| Business Logic Timing | ❌ Missing | Add `@Timed` annotations |
| JVM Memory/GC | ✅ Available | Already at `/metrics` |
| Thread Pool | ✅ Available | Already at `/metrics` |
| Container Resources | ✅ Available | cAdvisor at :8081/metrics |
| Host Resources | ✅ Available | node-exporter at :9100/metrics |
| PostgreSQL Internals | ❌ Missing | Start `postgres-exporter` |
| Distributed Traces | ❌ Broken | See QUARKUS_SIGNOZ_STATUS.md |

---

## Next Steps

1. **Immediate:** Enable Hibernate statistics or postgres-exporter
2. **Short-term:** Add `@Timed` annotations to business logic
3. **Long-term:** Fix OpenTelemetry trace export OR switch back to Tempo

---

**Which approach would you like to implement first?**
