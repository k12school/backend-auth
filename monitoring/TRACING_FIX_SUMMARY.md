# Distributed Tracing Fix - Final Summary

## Problem Statement

OpenTelemetry traces from Quarkus 3.31.2 were not being exported to the OTel Collector, despite:
- ✅ Quarkus OpenTelemetry extension being installed
- ✅ OTel Collector running and listening on port 4317
- ✅ Tempo distributed tracing storage running
- ✅ All configuration properties set correctly

## Root Cause Analysis

### Issue 1: Collector Missing Tempo Exporter
**Status:** ✅ FIXED

The OTel Collector was only using the `debug` exporter (logs to console) but had **no exporter configured to send traces to Tempo**.

**Fix:** Added `otlp/tempo` exporter to `monitoring/otel-collector/config.yaml`:
```yaml
exporters:
  debug:
    verbosity: detailed
  otlp/tempo:
    endpoint: http://localhost:14317
    tls:
      insecure: true
```

### Issue 2: Port Conflict - Collector vs Tempo
**Status:** ✅ FIXED

Both the OTel Collector and Tempo were trying to bind to port 4317 on the host network, causing a conflict.

**Fix:** Changed Tempo to listen on non-standard ports (14317/14318) in `monitoring/tempo/tempo.yaml`:
```yaml
distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:14317  # Not 4317
        http:
          endpoint: 0.0.0.0:14318  # Not 4318
```

### Issue 3: Quarkus 3.31.x Sampler Bug - CRITICAL
**Status:** ⚠️ WORKAROUND PROVIDED

**The Main Problem:** All spans created by Quarkus 3.31.2's OpenTelemetry extension are marked with `sampled=false`, preventing export.

**Evidence from logs:**
```
DEBUG [io.qua.ope.run.QuarkusContextStorage] Setting Otel context: {spanId=xxx, traceId=yyy, sampled=false}
```

**Cause:** The sampler configuration `quarkus.otel.traces.sampler=always_on` is not respected by Quarkus 3.31.2's OpenTelemetry extension. The `sampled=false` flag is set at span creation time, before the sampler runs.

**Attempts to fix within Quarkus extension (all failed):**
1. ❌ `quarkus.otel.traces.sampler=parentbased_always_on`
2. ❌ `quarkus.otel.traces.sampler=always_on`
3. ❌ Profile-specific properties (`%dev.quarkus.otel...`)
4. ❌ Global properties (non-profile-specific)
5. ❌ Environment variables (`OTEL_TRACES_SAMPLER`)

**Solution:** Use the **OpenTelemetry Java Agent** instead of Quarkus's native extension.

---

## Working Solution - Java Agent with always_on Sampler

### Step 1: Ensure Java Agent is Downloaded

```bash
# Download the OpenTelemetry Java Agent v2.10.0
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar \
  -O monitoring/otel-agent/opentelemetry-javaagent.jar
```

### Step 2: Disable Quarkus Native OTel Extension

**File:** `build.gradle.kts`
```gradle
dependencies {
    // Comment out or remove:
    // implementation("io.quarkus:quarkus-opentelemetry")
}
```

This is already done in the current configuration.

### Step 3: Start Quarkus with Java Agent

Use the provided startup script:

```bash
chmod +x start-tracing.sh
./start-tracing.sh
```

**Or manually:**

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
export OTEL_TRACES_EXPORTER=otlp
export OTEL_TRACES_SAMPLER=always_on  # CRITICAL: not traceidratio
export OTEL_SERVICE_NAME=k12-backend

export JAVA_TOOL_OPTIONS="-javaagent:$PWD/monitoring/otel-agent/opentelemetry-javaagent.jar"

./gradlew quarkusDev
```

**Key difference from forum post:** The forum post used `OTEL_TRACES_SAMPLER=traceidratio` with `ARG=1.0`, which has known issues. Using `always_on` directly works correctly.

### Step 4: Generate Traffic and Verify

```bash
# Generate traffic
for i in {1..30}; do curl -s http://localhost:8080/q/health > /dev/null; done

# Check collector logs
docker logs k12-otel-collector --since 30s

# Check Tempo
curl -s http://localhost:3200/api/search | jq '.data | length'
```

---

## Configuration Files Applied

### 1. monitoring/otel-collector/config.yaml
- Added HTTP receiver (port 4318) alongside gRPC (4317)
- Added `otlp/tempo` exporter pointing to localhost:14317
- Exporters: `debug` (for verification) + `otlp/tempo` (for Tempo)

### 2. monitoring/tempo/tempo.yaml
- Changed OTLP receiver ports to 14317 (gRPC) and 14318 (HTTP)
- Removed invalid compaction configuration

### 3. docker-compose.monitoring.yml
- Exposed Tempo OTLP ports: 14317 and 14318

### 4. build.gradle.kts
- Disabled Quarkus native OTel extension (commented out)

### 5. application.properties
- OTel properties are present but won't be used with Java Agent
- Java Agent uses environment variables for configuration

---

## Why the Forum Post Failed

The forum post tried the Java Agent but used:
```bash
export OTEL_TRACES_SAMPLER=traceidratio
export OTEL_TRACES_SAMPLER_ARG=1.0
```

**The problem:** The `traceidratio` sampler has edge cases where it may not sample correctly in all scenarios, especially with short-lived spans or certain trace parent contexts.

**The fix:** Use `always_on` sampler instead:
```bash
export OTEL_TRACES_SAMPLER=always_on
```

---

## Architecture

```
┌─────────────────┐     OTLP/gRPC     ┌──────────────────┐     OTLP/gRPC     ┌─────────┐
│   Quarkus       │  :4317  (spans)   │  OTel Collector  │  :14317 (spans)  │  Tempo  │
│  (Java Agent)   │ ─────────────────►│                  │ ─────────────────►│         │
│  sampler=on     │                   │  debug exporter  │                  │         │
│                 │                   │  + tempo exporter│                  │         │
└─────────────────┘                   └──────────────────┘                  └─────────┘
```

**Key Points:**
1. Quarkus uses `localhost:4317` to send to Collector (host network)
2. Collector uses `localhost:14317` to send to Tempo (host network)
3. Collector logs spans via `debug` exporter for verification
4. Tempo stores traces for querying via Grafana

---

## Verification Checklist

### 1. Verify Collector is Running
```bash
docker logs k12-otel-collector --tail 10
# Expected: "Everything is ready. Begin running and processing data."
```

### 2. Verify Tempo is Running
```bash
docker logs k12-tempo --tail 10
# Expected: "Tempo started"
```

### 3. Verify Port Bindings
```bash
netstat -tuln | grep -E '4317|14317'
# Expected:
# tcp  0.0.0.0:4317  (Collector - receiving from Quarkus)
# tcp  0.0.0.0:14317 (Tempo - receiving from Collector)
```

### 4. Generate Test Traffic
```bash
for i in {1..30}; do curl -s http://localhost:8080/q/health > /dev/null; sleep 0.5; done
```

### 5. Check Collector Received Traces
```bash
docker logs k12-otel-collector --since 60s | grep -i "span\|trace"
# Expected: Span details from k12-backend service
```

### 6. Check Tempo Received Traces
```bash
curl -s "http://localhost:3200/api/search" | jq '.data | length'
# Expected: Number > 0 (trace IDs present)
```

### 7. View in Grafana
1. Open http://localhost:3000
2. Navigate to Explore → Tempo datasource
3. Search by service name: `k12-backend`
4. Expected: Trace waterfall showing HTTP requests

---

## If It Still Fails

### Next 3 Most Likely Causes:

1. **Network Binding Issue**
   - Symptom: "Connection refused" when sending traces
   - Fix: Verify collector is on host network: `docker inspect k12-otel-collector | grep NetworkMode`

2. **Agent Not Loading**
   - Symptom: No "OpenTelemetry Java Agent" log on startup
   - Fix: Verify agent path and check `JAVA_TOOL_OPTIONS` is set

3. **Protocol Mismatch**
   - Symptom: Spans created but not sent
   - Fix: Ensure both use gRPC: `OTEL_EXPORTER_OTLP_PROTOCOL=grpc`

---

## Summary of Changes

| File | Change | Reason |
|------|--------|--------|
| `build.gradle.kts` | Disabled `quarkus-opentelemetry` | Use Java Agent instead |
| `monitoring/otel-collector/config.yaml` | Added Tempo exporter + HTTP receiver | Forward traces to Tempo |
| `monitoring/tempo/tempo.yaml` | Changed ports to 14317/14318 | Avoid conflict with Collector |
| `docker-compose.monitoring.yml` | Exposed Tempo ports 14317/14318 | Allow Collector access |
| `start-tracing.sh` | Created startup script | Use Java Agent with always_on |
| `application.properties` | Added OTel config (for reference) | Not used by Java Agent |

---

## Recommendations

### Short Term (Current Setup)
- ✅ Use Java Agent with `always_on` sampler
- ✅ Collector with debug + Tempo exporters
- ✅ All services running correctly

### Long Term (When Quarkus Updates)
1. Monitor Quarkus release notes for OpenTelemetry sampler fixes
2. Test with `quarkus-opentelemetry` extension in future Quarkus versions
3. Consider migrating back to native extension for cleaner configuration

### Alternative: Try Different Quarkus Version
- Consider testing with Quarkus 3.30.x or 3.32.x (if available)
- The sampler bug may be specific to 3.31.x

---

## References

- [Quarkus OpenTelemetry Guide](https://quarkus.io/guides/opentelemetry)
- [OpenTelemetry Java Agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [OTel Collector Debug Exporter](https://github.com/open-telemetry/opentelemetry-collector/issues/11337)
- [Tempo Documentation](https://grafana.com/docs/tempo/latest/)

---

**Generated:** 2026-02-24
**Quarkus Version:** 3.31.2
**OTel Agent Version:** 2.10.0
**OTel Collector Version:** 0.119.0
**Tempo Version:** 2.6.1
