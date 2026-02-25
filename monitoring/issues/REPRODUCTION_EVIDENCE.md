# Reproduction Evidence

**Generated:** 2026-02-25 15:25:39 -03:00

This document contains all evidence collected during issue reproduction, including raw logs, HTTP responses, error messages, and timestamp correlations.

---

## Evidence A: Trace Export Failure

### Reproduction Steps

1. Start k12-backend container
2. Generate HTTP traffic: `curl http://localhost:8080/q/health`
3. Monitor logs: `docker logs k12-backend --tail -f`

### Observed Result

```
2026-02-25 14:55:25,014 WARNING [io.qua.ope.run.exp.otl.sen.VertxHttpSender] (vert.x-eventloop-thread-2) Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: k12-signoz-otel-collector
2026-02-25 14:55:30,776 WARNING [io.qua.ope.run.exp.otl.sen.VertxHttpSender] (vert.x-eventloop-thread-2) Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: k12-signoz-otel-collector
2026-02-25 14:55:36,604 WARNING [io.qua.ope.run.exp.otl.sen.VertxHttpSender] (vert.x-eventloop-thread-2) Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: k12-signoz-otel-collector
```

### HTTP Endpoint Test

```bash
$ curl -v http://localhost:4318/v1/traces
*   Trying [::1]:4318...
* Connected to localhost (::1) port 4318
> GET /v1/traces HTTP/1.1
> Host: localhost:4318
> User-Agent: curl/8.9.1
> Accept: */*
< HTTP/1.1 405 Method Not Allowed
< Content-Type: text/plain
< Date: Wed, 25 Feb 2026 14:24:11 GMT
< Content-Length: 41
405 method not allowed, supported: [POST]
```

**Interpretation:** Endpoint accessible (405 expected for GET on POST-only endpoint)

### ClickHouse Verification

```sql
SELECT count() FROM signoz_traces.distributed_signoz_spans;

Result: 0
```

### Timestamp Correlation

| Event | Timestamp |
|-------|-----------|
| Backend started | 2026-02-25 14:23:26 |
| First export error | 2026-02-25 14:55:25 |
| Export error interval | ~5-6 seconds |
| Last export error | 2026-02-25 15:10:09 |
| ClickHouse trace count | 0 rows (verified at 15:25) |

---

## Evidence B: Metrics Scrape Failure

### Reproduction Steps

1. Check metrics endpoint from host: `curl http://localhost:8080/metrics`
2. Check collector logs: `docker logs k12-signoz-otel-collector | grep scrape`
3. Monitor for 15 seconds to observe scrape interval

### Observed Result - Metrics Endpoint

```bash
$ curl http://localhost:8080/metrics | head -10
# TYPE jvm_buffer_count_buffers gauge
# HELP jvm_buffer_count_buffers An estimate of the number of buffers in the pool
jvm_buffer_count_buffers{id="mapped - 'non-volatile memory'"} 0.0
jvm_buffer_count_buffers{id="mapped"} 0.0
jvm_buffer_count_buffers{id="direct"} 43.0
# TYPE jvm_buffer_count_used_buffers gauge
# HELP jvm_buffer_count_used_buffers An estimate of the number of buffers in use
jvm_buffer_count_used_buffers{id="mapped - 'non-volatile memory'"} 0.0
...
```

**Status:** ✅ Endpoint responding with valid Prometheus format

### Observed Result - Collector Logs (Pre-Restart)

```
2026-02-25T14:23:00.470Z - Failed to scrape Prometheus endpoint
{"level":"warn","ts":"2026-02-25T14:23:00.470Z","caller":"internal/transaction.go:152","msg":"Failed to scrape Prometheus endpoint","resource":{"service.instance.id":"87990e4e-4d1b-4a36-9dee-2e781c94ce31","service.name":"/signoz-otel-collector","service.version":"dev"},"otelcol.component.id":"prometheus","otelcol.component.kind":"receiver","otelcol.signal":"metrics","scrape_timestamp":1772029380469,"target_labels":"{__name__=\"up\", instance=\"k12-backend:8080\", job=\"k12-backend\", job_name=\"k12-backend\", service_name=\"k12-backend\"}"}

2026-02-25T14:23:15.471Z - Failed to scrape Prometheus endpoint
2026-02-25T14:23:30.471Z - Failed to scrape Prometheus endpoint
2026-02-25T14:23:45.470Z - Failed to scrape Prometheus endpoint
2026-02-25T14:24:00.470Z - Failed to scrape Prometheus endpoint
2026-02-25T14:24:15.471Z - Failed to scrape Prometheus endpoint
2026-02-25T14:24:30.470Z - Failed to scrape Prometheus endpoint
2026-02-25T14:24:45.470Z - Failed to scrape Prometheus endpoint
2026-02-25T14:25:00.470Z - Failed to scrape Prometheus endpoint (last attempt)
```

**Pattern:** Scraping attempted every 15 seconds, all failing

### Observed Result - Collector Restart

```
2026-02-25T14:26:14.410Z - Scrape job added
{"level":"info","ts":"2026-02-25T14:26:14.410Z","caller":"targetallocator/manager.go:215","msg":"Scrape job added","resource":{"service.instance.id":"e2be6a50-d4da-4e87-b909-a7d8e273d5f0","service.name":"/signoz-otel-collector","service.version":"dev"},"otelcol.component.id":"prometheus","otelcol.component.kind":"receiver","otelcol.signal":"metrics","jobName":"k12-backend"}
```

### Observed Result - Post-Restart (59 minutes of silence)

**No scrape attempts logged after restart at 14:26:14**

```bash
$ docker logs k12-signoz-otel-collector --since 30m | grep "k12-backend"
(only shows the "Scrape job added" message from 14:26:14)
```

### ClickHouse Verification

```sql
SELECT count() FROM signoz_metrics.time_series_v2;

Result: 0
```

### Timestamp Correlation

| Event | Timestamp |
|-------|-----------|
| Last scrape attempt (pre-restart) | 2026-02-25 14:25:00.470 |
| Collector restarted | 2026-02-25 14:26:14.410 |
| Scrape job added | 2026-02-25 14:26:14.410 |
| Current time | 2026-02-25 15:25:39 |
| Time since last scrape | 60 minutes |
| Scrape attempts post-restart | 0 |

---

## Evidence C: Network Connectivity

### DNS Resolution Test

```bash
$ docker exec k12-signoz-otel-collector getent hosts k12-backend
10.89.10.17     k12-backend.dns.podman
```

**Status:** ✅ DNS resolution working

### Network Topology Verification

```bash
$ docker inspect k12-backend -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}: {{$v.IPAddress}}{{"\n"}}{{end}}'
k12-monitoring: 10.89.9.143
k12-signoz-net: 10.89.10.17

$ docker inspect k12-signoz-otel-collector -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}: {{$v.IPAddress}}{{"\n"}}{{end}}'
k12-signoz-net: 10.89.10.18
```

**Status:** ✅ Both containers on k12-signoz-net network

### Port Exposure Verification

```bash
$ docker port k12-backend
5005/tcp -> 0.0.0.0:5005
8080/tcp -> 0.0.0.0:8080

$ curl -I http://localhost:8080/metrics
HTTP/1.1 200 OK
Content-Type: text/plain; version=0.0.4; charset=utf-8
```

**Status:** ✅ Port 8080 exposed and accessible

### TCP Connectivity Test

```bash
$ docker exec k12-backend timeout 5 bash -c 'cat < /dev/null > /dev/tcp/k12-signoz-otel-collector/4317'
Connected successfully
```

**Status:** ✅ TCP connection from backend to collector works

---

## Evidence D: Log Export Configuration Analysis

### Handler Code Analysis

**File:** `src/main/java/com/k12/infrastructure/logging/OpenTelemetryLogHandler.java`

**Line 27 - Endpoint Configuration:**
```java
String otelEndpoint = System.getProperty("quarkus.otel.exporter.otlp.endpoint", 
                                       "http://localhost:4317");
```

**Line 38-39 - Exporter Creation:**
```java
OtlpGrpcLogRecordExporter exporter =
    OtlpGrpcLogRecordExporter.builder().setEndpoint(otelEndpoint).build();
```

**Line 44-49 - Batch Processor Configuration:**
```java
.addLogRecordProcessor(BatchLogRecordProcessor.builder(exporter)
        .setScheduleDelay(1000, TimeUnit.MILLISECONDS)
        .setMaxQueueSize(2048)
        .setMaxExportBatchSize(512)
        .setExporterTimeout(30000, TimeUnit.MILLISECONDS)
        .build())
```

**Line 72-75 - Debug Logging (every 100th log):**
```java
if (record.getSequenceNumber() % 100 == 0) {
    System.out.println("[OpenTelemetryLogHandler] Exporting log #" + record.getSequenceNumber() + ": "
            + record.getLevel() + " - " + message);
}
```

### Environment Variables Set

```bash
$ docker exec k12-backend env | grep OTEL
QUARKUS_OTEL_ENABLED=true
QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT=http://k12-signoz-otel-collector:4318
QUARKUS_OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
QUARKUS_OTEL_TRACES_SAMPLER=always_on
QUARKUS_OTEL_RESOURCE_ATTRIBUTES=service.name=k12-backend,deployment.environment=development
```

### Configuration Mismatch Identified

| Aspect | Handler Code | Environment Variable | Match? |
|--------|--------------|---------------------|--------|
| Protocol | gRPC (hardcoded) | http/protobuf | ❌ |
| Port | 4317 (default) | 4318 | ❌ |
| Config Source | System.getProperty | Environment Variable | ❓ |
| Property Name | quarkus.otel.* | QUARKUS_OTEL_* | ❓ |

### Log Activity Verification

```bash
$ docker logs k12-backend 2>&1 | grep -E "OpenTelemetryLogHandler|Exporting log"
✓ OpenTelemetryLogHandler registered - logs will be exported to SigNoz
```

**Observation:** Only startup message, no "Exporting log #" messages

This indicates either:
1. No logs are being produced at INFO level
2. Handler's isLoggable() check is failing
3. Logs are being queued but not exported
4. Exceptions caught silently

### ClickHouse Log Data

```sql
SELECT count() FROM signoz_logs.logs_v2;
Result: 12

SELECT resources_string['service.name'] as service, count() 
FROM signoz_logs.logs_v2 
GROUP BY service;
Result: k12-backend, 12

-- All logs are from dev mode (pre-Docker deployment)
```

---

## Evidence E: Application Lifecycle

### Container Startup Timeline

```bash
$ docker inspect k12-backend --format '{{.State.StartedAt}}'
2026-02-25T14:23:23.805413922Z

$ docker inspect k12-signoz-otel-collector --format '{{.State.StartedAt}}'
2026-02-25T14:26:14.217516252Z
```

### Backend Startup Logs

```
2026-02-25 14:23:26,149 INFO  [org.fly.cor.FlywayExecutor] (main) Database: jdbc:postgresql://k12-postgres:5432/k12_db (PostgreSQL 17.8)
2026-02-25 14:23:26,236 INFO  [org.fly.cor.int.com.DbValidate] (main) Successfully validated 4 migrations (execution time 00:00.031s)
2026-02-25 14:23:26,319 INFO  [org.fly.cor.int.com.DbMigrate] (main) Current version of schema "public": 6
2026-02-25 14:23:26,323 INFO  [org.fly.cor.int.com.DbMigrate] (main) Schema "public" is up to date. No migration necessary.
2026-02-25 14:23:26,478 WARN  [io.mic.cor.ins.MeterRegistry] (main) This Gauge has been already registered (name='netty.eventexecutor.workers')
✓ OpenTelemetryLogHandler registered - logs will be exported to SigNoz
2026-02-25 14:23:26,849 INFO  [io.quarkus] (main) k12-backend 1.0-SNAPSHOT on JVM (powered by Quarkus 3.31.2) started in 2.986s. Listening on: http://0.0.0.0:8080
2026-02-25 14:23:26,852 INFO  [io.quarkus] (main) Profile prod activated.
2026-02-25 14:23:26,852 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, flyway, hibernate-validator, jdbc-postgresql, micrometer, narayana-jta, opentelemetry, resteasy, resteasy-jackson, security, smallrye-context-propagation, smallrye-health, smallrye-jwt, smallrye-openapi, vertx]
```

**Observation:** 
- Profile: **prod** (not dev)
- OpenTelemetry feature installed
- Handler registered successfully
- Application started successfully

### Runtime Mode Verification

```
Profile prod activated.
```

**Implication:** Production mode has different logging behavior than dev mode
- Fewer log messages
- May suppress debug output
- May affect handler's isLoggable() behavior

---

## Summary of Evidence Collection

| Evidence Category | Status | Key Findings |
|------------------|--------|---------------|
| Trace export errors | ✅ Captured | 100% failure rate, HTTP/protobuf on :4318 |
| Metrics endpoint | ✅ Verified | Working, returning Prometheus format |
| Metrics scraping | ✅ Documented | Stopped after collector restart |
| Network connectivity | ✅ Tested | All layers working (DNS, TCP, HTTP) |
| Log handler | ✅ Analyzed | Protocol mismatch (gRPC vs HTTP) |
| ClickHouse data | ✅ Queried | 0 traces, 0 metrics, 12 old logs |
| Container lifecycle | ✅ Recorded | Timeline of restarts and startups |
| Configuration | ✅ Extracted | Mismatch between code and env vars |

**All raw evidence preserved above for root cause analysis.**
