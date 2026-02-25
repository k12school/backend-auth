# Telemetry Issues List

**Generated:** 2026-02-25 15:25:39 -03:00  
**Environment:** Docker Compose (k12-backend + k12-postgres)  
**Status:** Observability DOWN (0% data collection)

---

## Issue #1: Trace Export Failures

**Classification:** Exporter failures  
**Component:** k12-backend (Quarkus)  
**Tool:** OpenTelemetry VertxHttpSender  
**Target:** k12-signoz-otel-collector:4318  
**Frequency:** ~1 error every 5-15 seconds when spans are available  
**Consistency:** Constant (100% failure rate)

### Exact Symptom

```
WARNING [io.qua.ope.run.exp.otl.sen.VertxHttpSender] (vert.x-eventloop-thread-2) 
Failed to export TraceRequestMarshaler. The request could not be executed. 
Full error message: k12-signoz-otel-collector
```

### Sample Timestamps

- 2026-02-25 14:55:25,014
- 2026-02-25 14:55:30,776
- 2026-02-25 14:55:36,604
- 2026-02-25 15:03:07,754
- 2026-02-25 15:10:09,061

### Pattern

Errors occur in batches every ~5 seconds, then pause for several minutes, suggesting batch export attempts with retry backoff.

### Configuration

```
QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT=http://k12-signoz-otel-collector:4318
QUARKUS_OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

### Data Verification

```sql
SELECT count() FROM signoz_traces.distributed_signoz_spans;
Result: 0
```

**Status:** ❌ 0% export success rate

---

## Issue #2: Metrics Scrape Failures

**Classification:** Scrape failures  
**Component:** k12-signoz-otel-collector (Prometheus receiver)  
**Target:** k12-backend:8080/metrics  
**Frequency:** Every 15 seconds (when scraping occurred)  
**Consistency:** Intermittent - scraping stopped after collector restart

### Exact Symptom

```
Failed to scrape Prometheus endpoint
target_labels: {__name__="up", instance="k12-backend:8080", job="k12-backend", 
                job_name="k12-backend", service_name="k12-backend"}
```

### Timeline

- **Last Successful Scrape:** 2026-02-25 14:25:00
- **Collector Restart:** 2026-02-25 14:26:14
- **Post-Restart Scrapes:** 0 attempts in 59 minutes

### Scrape Configuration

```yaml
job_name: k12-backend
static_configs:
  - targets: [k12-backend:8080]
    labels:
      service_name: k12-backend
      job_name: k12-backend
metrics_path: /metrics
scrape_interval: 15s
```

### Metrics Endpoint Status

```bash
$ curl http://localhost:8080/metrics | head -5
# TYPE jvm_buffer_count_buffers gauge
# HELP jvm_buffer_count_buffers An estimate of the number of buffers in the pool
jvm_buffer_count_buffers{id="mapped - 'non-volatile memory'"} 0.0
```

**Endpoint Status:** ✅ Responding correctly with Prometheus format

### Data Verification

```sql
SELECT count() FROM signoz_metrics.time_series_v2;
Result: 0
```

**Status:** ❌ 0% collection success rate (currently stopped)

---

## Issue #3: Log Export Not Producing

**Classification:** Logs not collected  
**Component:** k12-backend (OpenTelemetryLogHandler)  
**Target:** k12-signoz-otel-collector:4317 (gRPC)  
**Frequency:** No export activity observed  
**Consistency:** Constant (no logs exported from Docker container)

### Exact Symptoms

1. Handler registered successfully:
   ```
   ✓ OpenTelemetryLogHandler registered - logs will be exported to SigNoz
   ```

2. No "Exporting log #" debug messages in logs (code should print every 100th log)

3. No new logs in ClickHouse since Docker deployment

### Configuration Mismatch

**Handler Code (OpenTelemetryLogHandler.java:27):**
```java
String otelEndpoint = System.getProperty("quarkus.otel.exporter.otlp.endpoint", 
                                       "http://localhost:4317");
```

**Handler Code (OpenTelemetryLogHandler.java:38-39):**
```java
OtlpGrpcLogRecordExporter exporter =
    OtlpGrpcLogRecordExporter.builder().setEndpoint(otelEndpoint).build();
```

**Environment Variables Set:**
```
QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT=http://k12-signoz-otel-collector:4318
QUARKUS_OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

**Mismatch:** Handler uses gRPC on port 4317, environment specifies HTTP/protobuf on port 4318

### Data Verification

```sql
SELECT count() FROM signoz_logs.logs_v2;
Result: 12

SELECT resources_string['service.name'] as service, count() 
FROM signoz_logs.logs_v2 
GROUP BY service;
Result: k12-backend (all from dev mode, pre-Docker)
```

**Status:** ❌ 100% of Docker container logs lost

---

## Issue #4: Protocol Mismatch

**Classification:** Configuration incompatibility  
**Components:** OpenTelemetryLogHandler vs Quarkus OTLP configuration  
**Severity:** Blocking (causes Issue #3)

### Root Cause

The custom OpenTelemetryLogHandler is hardcoded to use:
- **Protocol:** gRPC
- **Port:** 4317 (default)
- **Configuration Source:** Java system property `quarkus.otel.exporter.otlp.endpoint`

While the Quarkus application is configured to use:
- **Protocol:** HTTP/protobuf
- **Port:** 4318
- **Configuration Source:** Environment variable `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT`

### Additional Mismatch

**Handler reads from:** `System.getProperty("quarkus.otel.exporter.otlp.endpoint")`  
**Environment variable:** `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT`

**Question:** Does Quarkus automatically map environment variables (with underscores) to system properties (with dots)?

**Evidence Gap:** Not verified what endpoint the handler is actually using

---

## Failure Classification Summary

| Issue | Category | Subcategory | Failure Mode | Frequency | Impact |
|-------|----------|-------------|--------------|----------|--------|
| Trace export | Exporter failure | Request execution | Every export attempt | 100% | Critical |
| Metrics scrape | Scrape failure | Collection stopped | After collector restart | 100% (currently) | Critical |
| Log export | Configuration mismatch | Protocol mismatch | Continuous | 100% | Critical |
| Protocol mismatch | Configuration incompatibility | gRPC vs HTTP | Continuous | 100% | Critical |

**Overall Observability Status:** **DOWN** (0% data collection from Docker container)
