# Bug Report: OpenTelemetry Java Agent BatchSpanProcessor does not export telemetry on Quarkus

## Summary

The OpenTelemetry Java Agent (versions 2.10.0, 2.23.0, and 2.25.0) fails to export telemetry via OTLP when running with Quarkus applications. The agent initializes correctly, creates spans, but the BatchSpanProcessor never establishes network connections or exports data.

## Environment

- **Agent Version**: Tested with 2.10.0, 2.23.0, and 2.25.0
- **Java Runtime**: Eclipse Temurin JDK 21.0.10+7-LTS (also tested with JDK 25)
- **Framework**: Quarkus 3.31.2
- **Collector**: SigNoz OTEL Collector (but issue is agent-side)
- **Deployment**: Docker container

## Expected Behavior

The BatchSpanProcessor worker thread should:
1. Accumulate spans in a batch
2. Export them to the configured OTLP endpoint every 5 seconds (scheduleDelayNanos=5000000000)
3. Establish TCP connections to the collector
4. Log export activity

## Actual Behavior

1. ✅ Agent loads and initializes
2. ✅ SDK auto-configures with OTLP exporters
3. ✅ Spans are created by instrumentation
4. ✅ BatchSpanProcessor worker thread exists
5. ❌ **No TCP connections established**
6. ❌ **No export logs generated**
7. ❌ **No data sent to collector**

Only the LoggingSpanExporter (console) works.

## Reproduction Steps

### 1. Application Setup

**build.gradle.kts:**
```kotlin
dependencies {
    implementation("io.opentelemetry:opentelemetry-api:1.45.0")
    implementation("io.opentelemetry:opentelemetry-context:1.45.0")
    // NOTE: NO SDK dependencies on classpath
}
```

**application.properties:**
```properties
quarkus.otel.enabled=false
quarkus.log.level=INFO
quarkus.log.category."io.opentelemetry".level=DEBUG
```

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/quarkus-app ./quarkus-app
COPY monitoring/opentelemetry/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/quarkus-app/quarkus-run.jar"]
```

**docker-compose.yml:**
```yaml
services:
  k12-backend:
    environment:
      JAVA_OPTS: "-javaagent:/app/opentelemetry-javaagent.jar -Dotel.java.global-autoconfigure.enabled=true -Dotel.javaagent.debug=true"
      OTEL_SERVICE_NAME: k12-backend
      OTEL_EXPORTER_OTLP_TRACES_ENDPOINT: http://k12-signoz-otel-collector:4318/v1/traces
      OTEL_EXPORTER_OTLP_METRICS_ENDPOINT: http://k12-signoz-otel-collector:4318/v1/metrics
      OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
      OTEL_TRACES_EXPORTER: otlp
      OTEL_METRICS_EXPORTER: otlp
      OTEL_LOGS_EXPORTER: none
      OTEL_TRACES_SAMPLER: always_on
```

### 2. Run the Application

```bash
docker compose up -d k12-backend
```

### 3. Generate Traffic

```bash
curl http://localhost:8080/health
curl http://localhost:8080/otel-test
```

### 4. Observe Logs

```bash
docker logs k12-backend 2>&1 | grep "opentelemetry"
```

## Observed Logs

### Agent Initializes Correctly:
```
[otel.javaagent] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.25.0
[otel.javaagent] DEBUG io.opentelemetry.javaagent.tooling.VersionLogger - Running on Java 21.0.10
```

### SDK Auto-Configures with OTLP Exporters:
```
[otel.javaagent] DEBUG io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder - Global OpenTelemetry set to OpenTelemetrySdk{...
  spanProcessor=MultiSpanProcessor{...
    spanProcessorsEnd=[
      BatchSpanProcessor{spanExporter=OtlpHttpSpanExporter{...
        endpoint=http://k12-signoz-otel-collector:4318/v1/traces, ...
        scheduleDelayNanos=5000000000, ...
      }, ...
    ], ...
  }, ...
}
```

### Spans Are Created:
```
[otel.javaagent] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /health' : ...
[otel.javaagent] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'k12_db' : ...
```

### But No Export Activity:
```bash
# No TCP connection established:
$ docker exec k12-backend cat /proc/net/tcp | grep 10E6
# (empty - port 4318 is hex 10E6)

# No export logs from BatchSpanProcessor:
$ docker logs k12-backend 2>&1 | grep "BatchSpanProcessor.*Worker"
[otel.javaagent] DEBUG ...Transformed io.opentelemetry.sdk.trace.export.BatchSpanProcessor$Worker
# (No "Exporting..." or "Sent spans..." messages)
```

## Verification That Network Works

Manual POST to collector succeeds:
```bash
$ docker exec k12-backend sh -c 'curl -X POST http://k12-signoz-otel-collector:4318/v1/traces -H "Content-Type: application/json" -d "{\"resourceSpans\":[]}" -v'

* Connected to k12-signoz-otel-collector (10.89.10.224) port 4318 (#0)
> POST /v1/traces HTTP/1.1
< HTTP/1.1 200 OK
```

## Additional Investigation

### What We've Tried:

1. ✅ Removed all OpenTelemetry SDK dependencies from classpath
2. ✅ Downgraded from JDK 25 to JDK 21 (compatibility test)
3. ✅ Disabled Quarkus native OTel (`quarkus.otel.enabled=false`)
4. ✅ Fixed agent logging configuration
5. ✅ Upgraded agent from 2.10.0 → 2.23.0 → 2.25.0
6. ✅ Tried various environment variable combinations
7. ✅ Verified network connectivity
8. ✅ Verified collector endpoint accepts POST requests
9. ✅ Reduced batch delay to 1 second (testing)
10. ✅ Removed LoggingSpanExporter to force OTLP

### What We Observed:

The BatchSpanProcessor worker thread exists but never performs exports. This suggests:
- The worker thread is blocked or waiting
- The span queue might be empty (spans not being added to BatchProcessor)
- A classloading issue in Quarkus prevents OkHttp from making requests

## Workaround

Switch to Quarkus native OpenTelemetry (no Java agent):
```kotlin
dependencies {
    implementation("io.quarkus:quarkus-opentelemetry")
}
```

With configuration in `application.properties`:
```properties
quarkus.otel.enabled=true
quarkus.otel.exporter.otlp.endpoint=http://k12-signoz-otel-collector:4318
quarkus.otel.traces.exporter=otlp
```

## Links

- Similar issues possibly related to Quarkus classloading model
- OpenTelemetry Java Agent GitHub: https://github.com/open-telemetry/opentelemetry-java-instrumentation

## Additional Context

This is a blocking issue for using the OpenTelemetry Java Agent with Quarkus applications. The agent provides valuable automatic instrumentation, but being unable to export telemetry makes it unusable in production environments.

The fact that:
1. Manual HTTP requests to the collector succeed
2. All configuration appears correct
3. The agent partially works (spans created, console logging works)
4. But the BatchProcessor never exports

...suggests a deeper integration issue, possibly related to how Quarkus's custom classloader (RunnerClassLoader) interacts with the agent's exported HTTP client (OkHttp).

## Proof Images/Screenshots

Available upon request. Can provide:
- Full agent initialization logs
- Network connection verification
- Configuration files
- Docker setup
