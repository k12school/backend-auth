# 🔍 Distributed Tracing Status & Resolution Path

## Current Status: ⚠️ BLOCKED

### What's Working ✅
1. **Tempo** - Running on `localhost:3200`
2. **OTel Collector** - Running on `localhost:4317` with debug exporter
3. **Quarkus OpenTelemetry** - Extension installed and creating trace IDs internally

### What's NOT Working ❌
**Traces are NOT being exported from Quarkus to the OTel collector**

---

## Root Cause

### The Issue
Quarkus 3.31 OpenTelemetry creates traces internally (visible in logs with `sampled=false`), but **does not export them** because:

1. **Sampler not sampling** - All traces show `sampled=false`
2. **Configuration properties not recognized** - Quarkus 3.31 shows warnings:
   ```
   Unrecognized configuration key "quarkus.opentelemetry.tracer.sampler"
   Unrecognized configuration key "quarkus.opentelemetry.tracer.exporter.otlp.endpoint"
   Unrecognized configuration key "quarkus.opentelemetry.enabled"
   ```

3. **Missing OTLP Exporter Dependency** - The `quarkus-opentelemetry` extension may not include the OTLP exporter by default in Quarkus 3.31

---

## Evidence

### Quarkus Logs (showing traces with sampled=false)
```
2026-02-24 19:49:06,547 DEBUG [io.qua.ope.run.QuarkusContextStorage]
Closing Otel context: {spanId=bb928dbab3baff38, traceId=036b7a59d9c02871fc0864b63b6f0979, sampled=false}
```

### Startup Warnings
```
Unrecognized configuration key "quarkus.opentelemetry.tracer.sampler" was provided; it will be ignored
Unrecognized configuration key "quarkus.opentelemetry.tracer.exporter.otlp.endpoint" was provided; it will be ignored
```

### OTel Collector Logs
```
Everything is ready. Begin running and processing data.
```
(No trace data received despite traffic being generated)

---

## Configuration Attempts

### Attempted Configuration (NOT WORKING)
```properties
quarkus.opentelemetry.enabled=true
quarkus.opentelemetry.tracer.exporter.otlp.endpoint=http://localhost:4317
quarkus.opentelemetry.tracer.sampler=on
quarkus.opentelemetry.tracer.exporter.otlp.protocol=grpc
```

### Startup Output
```
Installed features: [agroal, cdi, compose, flyway, hibernate-validator,
jdbc-postgresql, micrometer, narayana-jta, opentelemetry, resteasy,
resteasy-jackson, security, smallrye-context-propagation, smallrye-health,
smallrye-jwt, smallrye-openapi, swagger-ui, vertx]
```

Note: `opentelemetry` feature is installed, but properties are not recognized.

---

## Resolution Options

### Option 1: Add OTLP Exporter Dependency (RECOMMENDED)

Quarkus 3.31 may require a separate dependency for OTLP export:

```gradle
implementation("io.quarkus:quarkus-opentelemetry")
// May need additional:
implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.30.1")
```

### Option 2: Use Environment Variables

Some Quarkus 3.x configs use env vars instead of properties:
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
export OTEL_TRACES_SAMPLER=always_on
```

### Option 3: Use OpenTelemetry Java Agent (Alternative)

Instead of Quarkus extension, use the Java agent:
```bash
-javaagent:opentelemetry-javaagent.jar
```

With env vars:
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_SERVICE_NAME=k12-backend
```

### Option 4: Use MicroProfile OpenTracing (Deprecated)

If OTel doesn't work, fall back to OpenTracing (deprecated but stable):
```gradle
implementation("io.quarkus:quarkus-smallrye-opentracing")
```

---

## Current Build Configuration

### build.gradle.kts
```gradle
implementation("io.quarkus:quarkus-opentelemetry")
```

### application.properties
```properties
quarkus.opentelemetry.enabled=true
quarkus.opentelemetry.tracer.exporter.otlp.endpoint=http://localhost:4317
quarkus.opentelemetry.tracer.sampler=on
quarkus.opentelemetry.tracer.exporter.otlp.protocol=grpc
quarkus.log.category."io.opentelemetry".level=DEBUG
```

---

## Next Steps (Recommended Action Plan)

1. **Add OTLP exporter dependency** to `build.gradle.kts`:
   ```gradle
   implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.30.1")
   ```

2. **Rebuild and restart** Quarkus

3. **Verify properties are recognized** (no warnings in startup log)

4. **Generate traffic** and check:
   - Quarkus logs for `sampled=true`
   - OTel collector logs for trace data

5. **If still not working**, try environment variables approach

---

## Monitoring Stack Status

### Phase 1: ✅ COMPLETE
- [x] Prometheus - Scraping metrics from Quarkus
- [x] Grafana - 4 dashboards provisioned
- [x] Loki - Aggregating logs from Promtail
- [x] cAdvisor - Container metrics
- [x] Node Exporter - Host metrics

### Phase 2: 🟡 PARTIAL (75%)
- [x] Postgres Exporter - Database metrics (284 metrics)
- [x] Uptime Kuma - Monitoring host services
- [ ] Tempo - **BLOCKED** waiting for trace export from Quarkus

---

## Alternative: Skip Traces for Now

If distributed tracing proves difficult to configure, you still have full observability:
- ✅ **Metrics** - JVM, HTTP, Database via Prometheus/Grafana
- ✅ **Logs** - Application logs via Loki
- ✅ **Uptime** - Service availability via Uptime Kuma
- ✅ **Database Performance** - Postgres metrics via exporter

Distributed tracing provides request-level detail, but the above gives good coverage.

---

## Summary

**Status:** Quarkus 3.31 OpenTelemetry extension is installed and creating traces, but
configuration properties are not recognized, causing all traces to be marked `sampled=false`
and not exported.

**Likely Fix:** Add OTLP exporter dependency or use environment variables for configuration.

**Priority:** Medium - Other monitoring (metrics, logs, uptime) is working well.
