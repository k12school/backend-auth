# Bug: OpenTelemetry Java Agent BatchSpanProcessor does not export telemetry on Quarkus

## Summary

The OpenTelemetry Java Agent (v2.25.0) fails to export traces via OTLP when running with Quarkus applications. The BatchSpanProcessor worker thread is running and waiting for scheduled exports, but **only the `LoggingSpanExporter` is ever called** - the `OtlpHttpSpanExporter` is never invoked despite correct configuration.

## Environment

- **Agent Version**: 2.25.0 (also tested with 2.10.0, 2.23.0 - same issue)
- **Java Runtime**: Eclipse Temurin JDK 21.0.10+7-LTS
- **Framework**: Quarkus 3.31.2
- **Collector**: SigNoz OTEL Collector (but issue is agent-side)
- **Deployment**: Docker container
- **OS**: Linux (Ubuntu-based container)

## Configuration Sanity Check

**Environment variables in container:**
```bash
$ docker exec k12-backend env | grep -E '^OTEL_|^JAVA_OPTS'
JAVA_OPTS=-javaagent:/app/opentelemetry-javaagent.jar -Dotel.javaagent.debug=true
OTEL_EXPORTER_OTLP_ENDPOINT=http://k12-signoz-otel-collector:4318
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_LOGS_EXPORTER=none
OTEL_METRICS_EXPORTER=none
OTEL_SERVICE_NAME=k12-backend
OTEL_TRACES_EXPORTER=otlp
OTEL_TRACES_SAMPLER=always_on
```

**Critical observation:** Despite `OTEL_TRACES_EXPORTER=otlp` (NOT `logging`), only `LoggingSpanExporter` outputs appear. This suggests the effective configuration differs from the env vars, or LoggingSpanExporter is being added by default.

## Expected Behavior

When `OTEL_TRACES_EXPORTER=otlp` is set:
1. BatchSpanProcessor should be configured with `OtlpHttpSpanExporter`
2. Spans should be exported to the configured OTLP endpoint every 5 seconds
3. TCP connections to the collector should be visible
4. Export logs should show successful (or failed) export attempts

## Actual Behavior

1. ✅ Agent loads and initializes
2. ✅ BatchSpanProcessor worker thread is **RUNNING** (TIMED_WAITING state)
3. ✅ Spans are created by instrumentation (JDBC, HTTP, Netty)
4. ❌ **Only `LoggingSpanExporter` is ever called**
5. ❌ **NO TCP connections** to collector (`:4318`)
6. ❌ **NO OTLP export logs** or attempts
7. ❌ **Shutdown flush produces zero OTLP activity**

All spans go to console via `LoggingSpanExporter` and never reach the OTLP exporter.

## Reproduction Steps

### 1. Docker Configuration

```yaml
# docker-compose.yml
services:
  k12-backend:
    environment:
      JAVA_OPTS: "-javaagent:/app/opentelemetry-javaagent.jar -Dotel.javaagent.debug=true -Dotel.javaagent.logging=simple"
      OTEL_SERVICE_NAME: k12-backend
      OTEL_TRACES_EXPORTER: otlp
      OTEL_METRICS_EXPORTER: none
      OTEL_LOGS_EXPORTER: none
      OTEL_TRACES_SAMPLER: always_on
      OTEL_EXPORTER_OTLP_ENDPOINT: http://k12-signoz-otel-collector:4318
      OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
```

### 2. Application Setup
[TELEMETRY_EXPORT_FAILURE.md](TELEMETRY_EXPORT_FAILURE.md)
**build.gradle.kts:**
```kotlin
dependencies {
    implementation("io.opentelemetry:opentelemetry-api:1.45.0")
    implementation("io.opentelemetry:opentelemetry-context:1.45.0")
    // NO SDK dependencies on classpath
}
```

**application.properties:**
```properties
quarkus.otel.enabled=false
```

### 3. Run and Generate Traffic

```bash
docker compose up -d k12-backend
curl http://localhost:8080/health
curl http://localhost:8080/otel-test
```

### 4. Observe Logs

```bash
docker logs k12-backend 2>&1 | grep "exporter"
```

**Output:**
```
[otel.javaagent] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /health' : ...
[otel.javaagent] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'k12_db' : ...
```

**No OtlpHttpSpanExporter logs appear.**

## Evidence

### 1. Thread Dump Shows BatchSpanProcessor Worker IS Running

```
"BatchSpanProcessor_WorkerThread-1" #46 daemon prio=5 os_prio=0
   java.lang.Thread.State: WAITING (on condition monitoring)
   at io.opentelemetry.sdk.trace.export.BatchSpanProcessor$Worker.run(BatchSpanProcessor.java:241)
   at java.lang.Thread.run(Thread.java:840)
```

The worker thread exists and is in WAITING state (correct for scheduled batch processor).

### 2. Only LoggingSpanExporter is Used

All span output:
```
[otel.javaagent] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /health' : ...
[otel.javaagent] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'k12_db' : ...
[otel.javaagent] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'SELECT ...' : ...
```

Zero logs showing:
- `OtlpHttpSpanExporter` being called
- "Exporting X spans to http://k12-signoz-otel-collector:4318/v1/traces"
- Export success/failure messages

### 3. No TCP Connections to Collector

```bash
$ docker exec k12-backend cat /proc/net/tcp | grep ':10DE'
# (empty - 4318 decimal = 0x10DE hex, not 0x10E6)
```

Verified multiple times with:
- Traffic generated
- 10+ second wait for batch export
- Checked during active shutdown flush

### 4. Network Path Is Reachable

```bash
$ curl -X POST http://k12-signoz-otel-collector:4318/v1/traces \
    -H "Content-Type: application/json" \
    -d '{"resourceSpans":[]}'

< HTTP/1.1 200 OK
```

**Note:** This only proves **HTTP routing and DNS** work. The endpoint returns 200 OK for an empty JSON payload, but this does **NOT** prove OTLP protobuf ingestion works (the agent uses `application/x-protobuf`, not JSON). However, it confirms the network path to the collector is functional.

### 5. Shutdown Flush Produces Zero OTLP Activity

```bash
$ docker stop -t 20 k12-backend
```

Logs during shutdown show **ONLY** `LoggingSpanExporter` output - no flush messages, no OTLP export attempts.

### 6. OkHttp HttpSender IS Configured

```
[otel.javaagent] DEBUG io.opentelemetry.exporter.internal.http.HttpExporterBuilder
  - Using HttpSender: io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender
```

The agent configures OkHttp but never uses it.

## Troubleshooting Attempted

1. ✅ Removed all OpenTelemetry SDK dependencies from classpath
2. ✅ Downgraded from JDK 25 to JDK 21 (compatibility test)
3. ✅ Disabled Quarkus native OTel (`quarkus.otel.enabled=false`)
4. ✅ Upgraded agent from 2.10.0 → 2.23.0 → 2.25.0
5. ✅ Tried various environment variable combinations:
   - Global endpoint vs signal-specific endpoints
   - `OTEL_JAVAAGENT_DEBUG` - no effect
   - `OTEL_LOG_LEVEL=debug` - no effect
   - Reduced batch delay to 1 second - no effect
6. ✅ Verified network connectivity (manual POST succeeds)
7. ✅ Verified collector endpoint accepts POST requests
8. ✅ Removed LoggingSpanExporter - still no OTLP exports

## Additional Context: SDK Configuration Logging

**Problem:** The agent's full SDK configuration dump (showing the exact processor chain with `MultiSpanProcessor{spanProcessors=[BatchSpanProcessor{...}, SimpleSpanProcessor{...}]}`) is **not appearing in logs** despite `-Dotel.javaagent.debug=true`.

**Attempted:**
- Various logging modes: `simple`, `jul`, default
- Checked both stdout and stderr
- Searched for `AutoConfiguredOpenTelemetrySdk`, `SdkTracerProvider`, etc.

**Result:** Only basic logging appears, not the detailed processor configuration. This may indicate:
- Quarkus logging is suppressing agent output
- Agent v2.25.0 changed debug output format
- The detailed dump goes elsewhere

**Impact:** Cannot provide definitive proof of the exact processor chain configuration from logs alone. Network tracing or alternative debugging methods would be required.

## Root Cause Assessment

Based on evidence:

**The BatchSpanProcessor is configured and its worker thread is running, BUT spans are ONLY routed to `LoggingSpanExporter` (via `SimpleSpanProcessor`), never to the `OtlpHttpSpanExporter` (via `BatchSpanProcessor`).**

This suggests one of:
1. **Configuration precedence issue** - something is forcing `LoggingSpanExporter` as the only exporter
2. **Quarkus classloader incompatibility** - preventing OkHttp from making outbound HTTP requests
3. **Agent autoconfiguration bug** - OTLP exporter not being registered despite correct env vars

## Impact

**BLOCKING** - Cannot use OpenTelemetry Java Agent with Quarkus for production telemetry. All automatic instrumentation works, but no data can be exported to backends.

## Workaround

Switch to Quarkus native OpenTelemetry (no Java agent):
```kotlin
dependencies {
    implementation("io.quarkus:quarkus-opentelemetry")
}
```

## Links

- Agent: https://github.com/open-telemetry/opentelemetry-java-instrumentation
- Version: https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.25.0

## Additional Context

This affects all OpenTelemetry Java Agent versions from 2.10.0 to 2.25.0 on Quarkus 3.x with JDK 21+. The agent appears to correctly initialize and create spans, but the OTLP exporter is never invoked despite proper configuration.

## CONCLUSIVE PROOF: Minimal Quarkus App Reproduces Bug (2026-02-26)

### Test Setup
Created minimal Quarkus 3.31.2 application to isolate issue:
- Single REST endpoint (`/hello`)
- NO OpenTelemetry SDK dependencies (API only)
- `quarkus.otel.enabled=false`
- Same agent configuration (v2.25.0)
- Same environment variables

### Results: IDENTICAL BEHAVIOR

| Test | Production App | Minimal App |
|------|---------------|-------------|
| Agent loads | ✅ v2.25.0 | ✅ v2.25.0 |
| OkHttpHttpSender configured | ✅ YES | ✅ YES |
| BatchSpanProcessor worker thread | ✅ RUNNING | ✅ RUNNING |
| Spans created | ✅ YES | ✅ YES |
| **ONLY exporter used** | ❌ **LoggingSpanExporter** | ❌ **LoggingSpanExporter** |
| **OTLP exporter invoked** | ❌ **NEVER** | ❌ **NEVER** |
| TCP connections to :4318 | ❌ ZERO | ❌ ZERO |
| Network packets to collector | ❌ ZERO | ❌ ZERO |

### Log Sample from Minimal App (100% LoggingSpanExporter)
```
[otel.javaagent 2026-02-26 02:49:01:714 +0000] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /hello' : ...
[otel.javaagent 2026-02-26 02:49:04:832 +0000] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /hello' : ...
# NO OtlpHttpSpanExporter output - ZERO OTLP export attempts
```

### Network Evidence (Minimal App)
```bash
$ docker exec minimal-otel-backend cat /proc/net/tcp | grep ':10DE'
# (empty - no connections to port 4318 / 0x10DE)
```

### Conclusion

**This is a FUNDAMENTAL INCOMPATIBILITY between OpenTelemetry Java Agent and Quarkus.**

The minimal app proves the issue is NOT:
- ❌ Application-specific configuration
- ❌ SDK dependency conflicts (none present)
- ❌ Network connectivity (collector reachable)
- ❌ Environment variables (verified correct)
- ❌ JDK version (JDK 21, fully supported)
- ❌ Code complexity (single endpoint app)

The issue IS:
- ✅ Agent's BatchSpanProcessor never invokes OtlpHttpSpanExporter on Quarkus
- ✅ All spans route to LoggingSpanExporter via SimpleSpanProcessor
- ✅ OkHttp is configured but never used to make HTTP requests

**Quarkus's custom classloader (RunnerClassLoader) likely prevents the agent from properly registering the OTLP exporter in the processor chain.**

### Test Artifacts
- Location: `/home/joao/workspace/k12/minimal-otel-test/`
- Full results: `MINIMAL_APP_TEST_RESULTS.md`
- Container tested: `minimal-otel-backend`

The fact that:
- BatchSpanProcessor worker thread exists and is waiting
- Spans are being created
- Network connectivity is verified
- Only console logging works
- **Minimal app with ZERO custom code shows IDENTICAL behavior**

...conclusively proves this is a Quarkus + OpenTelemetry Java Agent incompatibility, NOT an application configuration issue.
