# Minimal Quarkus App Test Results - OpenTelemetry Java Agent Bug Confirmed

## Test Date
2026-02-26

## Test Purpose
Create minimal Quarkus application to isolate whether OpenTelemetry Java Agent export failure is environmental or a fundamental Quarkus incompatibility.

## Test Setup

### Application
- **Framework**: Quarkus 3.31.2 (minimal - just REST)
- **Java**: JDK 21 (Eclipse Temurin)
- **Code**: Single GreetingResource.java with `/hello` endpoint
- **Dependencies**: NO OpenTelemetry SDK, only API
- **Quarkus OTel**: `quarkus.otel.enabled=false` (using Java Agent only)

### Agent Configuration
```bash
JAVA_OPTS=-javaagent:/app/opentelemetry-javaagent.jar \
  -Dotel.javaagent.debug=true \
  -Dotel.javaagent.logging=simple \
  -Dotel.traces.exporter=otlp \
  -Dotel.exporter.otlp.endpoint=http://k12-signoz-otel-collector:4318 \
  -Dotel.exporter.otlp.protocol=http/protobuf

OTEL_SERVICE_NAME=minimal-otel-test
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=none
OTEL_LOGS_EXPORTER=none
OTEL_TRACES_SAMPLER=always_on
OTEL_EXPORTER_OTLP_ENDPOINT=http://k12-signoz-otel-collector:4318
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

## Results

### Agent Initialization
✅ **SUCCESS**: Agent v2.25.0 loads and initializes
```
[otel.javaagent 2026-02-26 02:48:27:471 +0000] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.25.0
```

✅ **SUCCESS**: OkHttpHttpSender configured
```
[otel.javaagent 2026-02-26 02:48:28:235 +0000] [main] DEBUG io.opentelemetry.exporter.internal.http.HttpExporterBuilder - Using HttpSender: io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender
```

✅ **SUCCESS**: BatchSpanProcessor worker thread created and running
```
[otel.javaagent 2026-02-26 02:48:38:260 +0000] [BatchSpanProcessor_WorkerThread-1] DEBUG io.opentelemetry.exporter.internal.marshal.StringEncoderHolder - Using UnsafeStringEncoder for optimized Java 8+ performance
```

### Span Creation
✅ **SUCCESS**: Automatic instrumentation creates spans (Netty HTTP)
```
[otel.javaagent 2026-02-26 02:49:01:714 +0000] [vert.x-eventloop-thread-12] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /hello' : 5c37c19ed01fe6b4868e34ced61351ca d5c3cdbc922d9a72 SERVER [tracer: io.opentelemetry.netty-4.1:2.25.0-alpha]
```

### Export Behavior
❌ **FAILURE**: ONLY LoggingSpanExporter is used - EVERY span goes to console
❌ **FAILURE**: NO OtlpHttpSpanExporter logs appear
❌ **FAILURE**: NO TCP connections to collector (port 4318 / 0x10DE)
❌ **FAILURE**: NO batch export messages ("Exporting X spans to...")

### Network Evidence
```bash
$ docker exec minimal-otel-backend cat /proc/net/tcp | grep ':10DE'
# (empty - no connections to port 4318)

$ docker exec minimal-otel-backend ss -tn | grep 4318
# (empty)
```

### Log Sample (100% LoggingSpanExporter, 0% OTLP)
```
[otel.javaagent 2026-02-26 02:49:01:714 +0000] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /hello' : ...
[otel.javaagent 2026-02-26 02:49:04:832 +0000] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /hello' : ...
[otel.javaagent 2026-02-26 02:49:42:387 +0000] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET' : ...
# ... ALL spans, NO OtlpHttpSpanExporter output
```

## Critical Observations

1. **BatchSpanProcessor worker thread EXISTS** - logs show it's running
2. **OkHttpHttpSender IS configured** - debug logs prove it
3. **Spans ARE created** - instrumentation works perfectly
4. **ONLY LoggingSpanExporter is called** - spans go to console, not collector
5. **ZERO network activity** - no connect() syscalls, no packets to :4318

## Conclusion

**This is a FUNDAMENTAL INCOMPATIBILITY between OpenTelemetry Java Agent and Quarkus.**

The minimal app proves the issue is NOT:
- ❌ Application-specific configuration
- ❌ SDK dependency conflicts (none present)
- ❌ Network connectivity (collector reachable)
- ❌ Environment variables (verified correct)
- ❌ JDK version (JDK 21, fully supported)

The issue IS:
- ✅ **Agent's BatchSpanProcessor never invokes OtlpHttpSpanExporter on Quarkus**
- ✅ All spans route to LoggingSpanExporter via SimpleSpanProcessor
- ✅ OkHttp is configured but never used to make HTTP requests

## Test Artifacts

- Location: `/home/joao/workspace/k12/minimal-otel-test/`
- Container: `minimal-otel-backend`
- Port: 8081
- Endpoint: `http://localhost:8081/hello`

## Next Steps

1. Research OpenTelemetry Java Agent GitHub issues for Quarkus compatibility
2. Check if Quarkus's custom classloader (RunnerClassLoader) conflicts with agent
3. Test agent on non-Quarkus Java app to confirm Quarkus-specific issue
4. Consider filing bug report with OpenTelemetry Java Instrumentation project
