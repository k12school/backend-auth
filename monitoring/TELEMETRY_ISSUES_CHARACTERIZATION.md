# Telemetry Issues Characterization Report
## Generated: 2026-02-25 15:25:39 -03:00

---

## 1. ISSUE LIST

### Issue #1: Trace Export Failures
**Classification:** Exporter failures  
**Component:** k12-backend (Quarkus)  
**Tool:** OpenTelemetry VertxHttpSender  
**Target:** k12-signoz-otel-collector:4318  
**Frequency:** ~1 error every 5-15 seconds when spans are available  
**Consistency:** Constant (100% failure rate)

**Exact Symptom:**
```
WARNING [io.qua.ope.run.exp.otl.sen.VertxHttpSender] (vert.x-eventloop-thread-2) 
Failed to export TraceRequestMarshaler. The request could not be executed. 
Full error message: k12-signoz-otel-collector
```

**Sample Timestamps:**
- 2026-02-25 14:55:25,014
- 2026-02-25 14:55:30,776
- 2026-02-25 14:55:36,604
- 2026-02-25 15:03:07,754
- 2026-02-25 15:10:09,061

**Pattern:** Errors occur in batches every ~5 seconds, then pause for several minutes, suggesting batch export attempts with retry backoff.

---

### Issue #2: Metrics Scrape Failures
**Classification:** Scrape failures  
**Component:** k12-signoz-otel-collector (Prometheus receiver)  
**Target:** k12-backend:8080/metrics  
**Frequency:** Every 15 seconds (when scraping occurred)  
**Consistency:** Intermittent - scraping stopped after collector restart

**Exact Symptom:**
```
Failed to scrape Prometheus endpoint
target_labels: {__name__="up", instance="k12-backend:8080", job="k12-backend", 
                job_name="k12-backend", service_name="k12-backend"}
```

**Last Successful Scrape:** 2026-02-25 14:25:00  
**Collector Restart:** 2026-02-25 14:26:14  
**Post-Restart Scrapes:** 0 attempts in 59 minutes

**Scrape Configuration:**
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

---

### Issue #3: Log Export Not Producing
**Classification:** Logs not collected  
**Component:** k12-backend (OpenTelemetryLogHandler)  
**Target:** k12-signoz-otel-collector:4317 (gRPC)  
**Frequency:** No export activity observed  
**Consistency:** Constant (no logs exported from Docker container)

**Exact Symptom:**
- Handler registered successfully: "✓ OpenTelemetryLogHandler registered - logs will be exported to SigNoz"
- No "Exporting log #" debug messages in logs
- No new logs in ClickHouse since Docker deployment

**Configuration Mismatch:**
- Handler code uses: `OtlpGrpcLogRecordExporter` (hardcoded gRPC on port 4317)
- Environment variable: `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT=http://k12-signoz-otel-collector:4318`
- Handler reads from: `System.getProperty("quarkus.otel.exporter.otlp.endpoint")` (may not map from env var)

**Data in ClickHouse:**
- Total logs: 12
- All from: k12-backend (dev mode, pre-Docker)
- No logs from Docker containerized application

---

### Issue #4: Protocol Mismatch
**Classification:** Configuration incompatibility  
**Components:** OpenTelemetryLogHandler vs Quarkus OTLP configuration  
**Severity:** Blocking (causes Issue #3)

**Exact Symptom:**
- Traces configured: `http/protobuf` on port 4318
- Log handler hardcoded: gRPC on port 4317
- Custom handler doesn't read Quarkus environment variables

---

## 2. REPRODUCTION EVIDENCE

### Evidence A: Trace Export Failure
**Reproduction Steps:**
1. Start k12-backend container
2. Generate HTTP traffic: `curl http://localhost:8080/q/health`
3. Monitor logs: `docker logs k12-backend --tail -f`

**Observed Result:**
```
2026-02-25 14:55:25,014 WARNING [io.qua.ope.run.exp.otl.sen.VertxHttpSender] 
Failed to export TraceRequestMarshaler. The request could not be executed. 
Full error message: k12-signoz-otel-collector
```

**HTTP Endpoint Test:**
```bash
$ curl -v http://localhost:4318/v1/traces
> GET /v1/traces HTTP/1.1
< HTTP/1.1 405 Method Not Allowed
< Content-Type: text/plain
< 405 method not allowed, supported: [POST]
```
Endpoint accessible (405 expected for GET on POST endpoint)

**ClickHouse Verification:**
```sql
SELECT count() FROM signoz_traces.distributed_signoz_spans;
Result: 0
```

---

### Evidence B: Metrics Scrape Failure
**Reproduction Steps:**
1. Check metrics endpoint from host: `curl http://localhost:8080/metrics`
2. Check collector logs: `docker logs k12-signoz-otel-collector | grep scrape`
3. Monitor for 15 seconds

**Observed Result - Metrics Endpoint:**
```
# TYPE jvm_buffer_count_buffers gauge
# HELP jvm_buffer_count_buffers An estimate of the number of buffers in the pool
jvm_buffer_count_buffers{id="mapped - 'non-volatile memory'"} 0.0
...
```
Metrics endpoint **IS responding** with Prometheus format

**Observed Result - Collector Logs (pre-restart):**
```
2026-02-25T14:23:00.470Z - Failed to scrape Prometheus endpoint
2026-02-25T14:23:15.471Z - Failed to scrape Prometheus endpoint
2026-02-25T14:23:30.471Z - Failed to scrape Prometheus endpoint
...
2026-02-25T14:25:00.470Z - Failed to scrape Prometheus endpoint (last attempt)
```

**Observed Result - Post-Restart:**
```
2026-02-25T14:26:14.410Z - Scrape job added (k12-backend)
[59 minutes of silence - no scrape attempts]
```

**ClickHouse Verification:**
```sql
SELECT count() FROM signoz_metrics.time_series_v2;
Result: 0
```

---

### Evidence C: Network Connectivity
**Test from collector to backend:**
```bash
$ docker exec k12-signoz-otel-collector getent hosts k12-backend
10.89.10.17     k12-backend.dns.podman
```
DNS resolution: ✅ Working

**Network Topology:**
```
k12-backend:
  - k12-monitoring: 10.89.9.143
  - k12-signoz-net: 10.89.10.17

k12-signoz-otel-collector:
  - k12-signoz-net: 10.89.10.18
```
Both on k12-signoz-net: ✅

**Port Exposure:**
```
k12-backend ports:
  5005/tcp -> 0.0.0.0:5005
  8080/tcp -> 0.0.0.0:8080
```
Port 8080 exposed: ✅

---

### Evidence D: Log Export Configuration
**Handler Code Analysis:**
```java
// Line 27: Reads from system property (not environment variable)
String otelEndpoint = System.getProperty("quarkus.otel.exporter.otlp.endpoint", 
                                       "http://localhost:4317");

// Line 38-39: Hardcoded gRPC exporter
OtlpGrpcLogRecordExporter exporter =
    OtlpGrpcLogRecordExporter.builder().setEndpoint(otelEndpoint).build();
```

**Environment Variables Set:**
```
QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT=http://k12-signoz-otel-collector:4318
QUARKUS_OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

**Mismatch:** Handler uses gRPC :4317, env var specifies http/protobuf :4318

---

## 3. SIGNAL CORRELATION MATRIX

| Issue | Producing Component | Transport Layer | Collector | Storage | UI | Status |
|-------|-------------------|-----------------|-----------|---------|-----|--------|
| **Traces** | k12-backend (Quarkus OTLP) | HTTP/protobuf on :4318 | k12-signoz-otel-collector | ❌ 0 rows | ❌ | ❌ Export failing |
| **Metrics** | k12-backend (Prometheus endpoint :8080) | HTTP scrape on :8080 | k12-signoz-otel-collector | ❌ 0 rows | ❌ | ⚠️ Scrape stopped |
| **Logs** | k12-backend (Custom LogHandler) | gRPC on :4317 (intended) | k12-signoz-otel-collector | ⚠️ 12 old rows | ❌ | ❌ Config mismatch |

**Legend:**
- ✅ Working
- ⚠️ Partial/Intermittent
- ❌ Not working

---

## 4. IMPACT ASSESSMENT

### Data Loss Quantification

**Traces:**
- 0% of traces successfully exported
- 100% export failure rate
- Impact: No distributed tracing visibility, no span data in SigNoz UI

**Metrics:**
- 0% of metrics collected
- Scraping stopped after collector restart (59 minutes ago)
- Impact: No application metrics in dashboards, no performance monitoring

**Logs:**
- 100% of logs from Docker container lost
- Only 12 old logs from dev mode remain in database
- Impact: No log aggregation, cannot debug production issues

### Observability Coverage

| Signal | In Dev Mode | In Docker | Coverage Gap |
|--------|-------------|-----------|--------------|
| Traces | ❌ (not tested) | ❌ | Unknown |
| Metrics | ❌ (not tested) | ❌ | Unknown |
| Logs | ✅ (12 old logs) | ❌ | 100% loss in Docker |

---

## 5. UNKNOWNS / GAPS

### Unknown #1: Root Cause of Trace Export Failure
**Question:** Why does the trace export fail with "The request could not be executed"?

**Known:**
- HTTP endpoint is accessible (returns 405 for GET, which is correct)
- Network connectivity is confirmed (both containers on same network)
- DNS resolution working

**Unknown:**
- What is the actual HTTP error code?
- What is the request payload?
- Is the collector rejecting the request (400/500 error)?
- Is the request timing out?

**Evidence Gap:** No HTTP request/response logs from VertxHttpSender

---

### Unknown #2: Why Metrics Scraping Stopped After Restart
**Question:** Why did Prometheus scraping stop after the collector restart at 14:26:14?

**Known:**
- Scrape job was added successfully
- Scraping was happening before restart (every 15 seconds)
- Metrics endpoint is accessible and responding
- Backend and collector are on same network

**Unknown:**
- Did the scrape manager crash silently?
- Is there a configuration validation error?
- Is there a permission issue accessing the backend?
- Are there internal collector errors not being logged?

**Evidence Gap:** No error logs after "Scrape job added"

---

### Unknown #3: Log Handler Activity
**Question:** Is the OpenTelemetryLogHandler being called at all?

**Known:**
- Handler registered successfully
- Application is running in prod mode
- Application logs exist (Quarkus startup logs visible)

**Unknown:**
- Are INFO-level logs being produced during runtime?
- Is the handler's isLoggable() check passing?
- Are logs being queued but not exported?
- Are there exceptions being caught silently?

**Evidence Gap:** No "Exporting log #" debug messages in logs

---

### Unknown #4: Quarkus to System Property Mapping
**Question:** Do Quarkus environment variables automatically map to Java system properties?

**Known:**
- Environment variables are set (QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT)
- Handler reads from System.getProperty("quarkus.otel.exporter.otlp.endpoint")
- No manual mapping code found

**Unknown:**
- Does Quarkus automatically map env vars to system properties?
- Is the handler getting the default value "http://localhost:4317"?
- Is there a naming convention mismatch (underscores vs dots)?

**Evidence Gap:** Haven't verified what endpoint the handler is actually using

---

## 6. FAILURE CLASSIFICATION SUMMARY

| Issue | Category | Subcategory | Failure Mode | Frequency | Impact |
|-------|----------|-------------|--------------|----------|--------|
| Trace export | Exporter failure | Request execution | Every export attempt | 100% | Critical |
| Metrics scrape | Scrape failure | Collection stopped | After collector restart | 100% (currently) | Critical |
| Log export | Configuration mismatch | Protocol mismatch | Continuous | 100% | Critical |
| Protocol mismatch | Configuration incompatibility | gRPC vs HTTP | Continuous | 100% | Critical |

**Overall Observability Status:** **DOWN** (0% data collection from Docker container)
