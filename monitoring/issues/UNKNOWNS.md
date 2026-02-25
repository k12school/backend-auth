# Unknowns and Evidence Gaps

**Generated:** 2026-02-25 15:25:39 -03:00

This document identifies what we don't know and what evidence we need to gather to complete root cause analysis.

---

## Unknown #1: Root Cause of Trace Export Failure

### Question
Why does the trace export fail with "The request could not be executed"?

### What We Know

✅ **Confirmed:**
- HTTP endpoint is accessible (returns 405 for GET, which is correct)
- Network connectivity is confirmed (both containers on same network)
- DNS resolution working (k12-backend ↔ k12-signoz-otel-collector)
- TCP connection successful (tested with bash /dev/tcp)
- Quarkus configuration appears correct (http/protobuf on :4318)
- Exporter is attempting sends (errors every 5-15 seconds)

❌ **Unknown:**
- What is the actual HTTP error code (400, 500, timeout)?
- What is the request payload size?
- Is the collector rejecting the request?
- Is the request timing out (30s timeout configured)?
- Is there a protocol version mismatch?
- Is there authentication missing?
- Are headers missing or incorrect?

### Evidence Gap

**Missing:**
- No HTTP request/response logs from VertxHttpSender
- No collector ingestion logs for trace requests
- No network packet captures
- No HTTP debug logs showing actual request/response

### Evidence Needed

1. **Enable HTTP request logging:**
   - Set Quarkus OTLP logging to TRACE
   - Capture actual HTTP request headers
   - Capture HTTP response codes
   - Log request payload size

2. **Collector-side logging:**
   - Enable OTLP receiver debug logs
   - Check for rejected requests
   - Look for 400/500 errors
   - Verify protocol compatibility

3. **Network-level capture:**
   - tcpdump on :4318
   - Wireshark capture of HTTP traffic
   - Verify HTTP/2 vs HTTP/1.1
   - Check TLS/SSL issues

### Investigation Commands

```bash
# Enable detailed OTLP logging in Quarkus
# Add to application.properties or environment:
QUARKUS_LOG_CATEGORY_"io.opentelemetry".level=TRACE
QUARKUS_LOG_CATEGORY_"io.quarkus.opentelemetry".level=TRACE
QUARKUS_LOG_CATEGORY_"io.vertx".level=TRACE

# Check collector receiver status
docker logs k12-signoz-otel-collector | grep -i "otlp.*receiver"

# Network capture
docker exec k12-backend tcpdump -i any port 4318 -w /tmp/capture.pcap
```

---

## Unknown #2: Why Metrics Scraping Stopped After Restart

### Question
Why did Prometheus scraping stop after the collector restart at 14:26:14?

### What We Know

✅ **Confirmed:**
- Scrape job was added successfully at 14:26:14
- Scraping was happening before restart (every 15 seconds)
- Last scrape attempt: 14:25:00.470
- Metrics endpoint is accessible and responding
- Backend and collector are on same network (k12-signoz-net)
- DNS resolution working (k12-backend → 10.89.10.17)
- Configuration is correct (static target, correct port)

❌ **Unknown:**
- Did the scrape manager crash silently?
- Is there a configuration validation error?
- Is there a permission issue accessing the backend?
- Are there internal collector errors not being logged?
- Did the target health check fail?
- Is there a race condition in service discovery?
- Did the scrape interval get set to 0 or infinite?

### Evidence Gap

**Missing:**
- No error logs after "Scrape job added"
- No scrape manager state logs
- No target health check results
- No internal collector diagnostics
- No Prometheus receiver status updates

### Timeline Analysis

| Time | Event | Expected | Actual |
|------|-------|----------|--------|
| 14:23:00 | Scrape started | Every 15s | ✅ Working |
| 14:25:00 | Last scrape | Scrape #4 | ✅ Last success |
| 14:26:14 | Collector restart | Scraping resumes | ❌ Never resumed |
| 14:26:29 | Expected scrape | 15s after restart | ❌ Didn't happen |
| 14:26:44 | Expected scrape | 30s after restart | ❌ Didn't happen |

**Gap:** 59 minutes with 0 scrape attempts

### Evidence Needed

1. **Collector health status:**
   ```bash
   # Check if scrape manager is running
   docker exec k12-signoz-otel-collector curl http://localhost:13133
   ```

2. **Scrape manager state:**
   - Check if job is in "active" state
   - Verify scrape interval is not 0
   - Check for internal errors

3. **Target reachability from collector:**
   - Manual HTTP request from collector to backend
   - Check firewall rules within collector
   - Verify no IP blocks

4. **Configuration reload status:**
   - Did config reload complete successfully?
   - Was the scrape job actually added to the manager?
   - Is there a syntax error in the config?

### Investigation Commands

```bash
# Check scrape manager health
docker exec k12-signoz-otel-collector wget -qO- http://localhost:8888/metrics | grep scrape

# Check collector internal state
docker exec k12-signoz-otel-collector cat /etc/otel-collector-config.yaml | grep -A 10 k12-backend

# Manual scrape attempt from collector
docker exec k12-signoz-otel-collector wget -qO- http://k12-backend:8080/metrics | head -5
```

---

## Unknown #3: Log Handler Activity

### Question
Is the OpenTelemetryLogHandler being called at all?

### What We Know

✅ **Confirmed:**
- Handler registered successfully (startup log visible)
- Application is running in prod mode
- Application logs exist (Quarkus startup logs visible)
- Handler code looks correct (implements Handler interface)

❌ **Unknown:**
- Are INFO-level logs being produced during runtime?
- Is the handler's isLoggable() check passing?
- Are logs being queued but not exported?
- Are there exceptions being caught silently?
- Is the log level filter blocking INFO logs in prod mode?
- Did the handler attach to the correct logger?

### Code Analysis

```java
// Line 56-59: Filtering check
@Override
public void publish(LogRecord record) {
    if (!isLoggable(record) || otelLogger == null) {
        return;  // Early return - no export
    }
```

**Possible failure points:**
1. `isLoggable(record)` returning false
2. `otelLogger` is null
3. Log level doesn't meet threshold

### Evidence Gap

**Missing:**
- No "Exporting log #" debug messages (should print every 100th log)
- No exception logs from handler
- No visibility into isLoggable() logic
- Don't know what log level Quarkus uses in prod mode

### Evidence Needed

1. **Verify log production:**
   - Generate INFO-level logs in production
   - Check if handler.publish() is called
   - Add debug logging to handler

2. **Check log levels:**
   - What is the root logger level in prod mode?
   - Are INFO logs actually being produced?
   - Is the handler attached to the root logger?

3. **Handler lifecycle:**
   - Is otelLogger null?
   - Did the exporter initialize correctly?
   - Are there exceptions in constructor?

### Investigation Commands

```bash
# Check current log level
docker logs k12-backend 2>&1 | grep "LOG_CATEGORY"

# Generate test log and check handler
curl http://localhost:8080/api/tenants 2>&1 | head -5
docker logs k12-backend --tail 20 | grep -v "Failed to export"

# Check if any INFO logs are produced
docker logs k12-backend 2>&1 | grep " INFO " | wc -l
```

### Code Instrumentation Needed

Add to OpenTelemetryLogHandler:

```java
@Override
public void publish(LogRecord record) {
    System.out.println("[DEBUG] Handler.publish() called for: " + record.getLevel());
    
    if (!isLoggable(record)) {
        System.out.println("[DEBUG] Record not loggable, skipping");
        return;
    }
    
    if (otelLogger == null) {
        System.out.println("[DEBUG] otelLogger is NULL!");
        return;
    }
    
    System.out.println("[DEBUG] Exporting log: " + record.getMessage());
    // ... rest of code
}
```

---

## Unknown #4: Quarkus to System Property Mapping

### Question
Do Quarkus environment variables automatically map to Java system properties?

### What We Know

✅ **Confirmed:**
- Environment variables are set (verified with `docker exec k12-backend env`)
- Handler code reads from `System.getProperty("quarkus.otel.exporter.otlp.endpoint")`
- No manual mapping code found in handler
- Default value in code: `http://localhost:4317`

❌ **Unknown:**
- Does Quarkus automatically map `QUARKUS_OTEL_*` env vars to `quarkus.otel.*` system properties?
- Is the handler getting the default value "http://localhost:4317"?
- Is there a naming convention mismatch (underscores vs dots)?
- Does the mapping happen at application startup or runtime?
- Is there a conversion pattern Quarkus uses?

### Configuration Sources

**Environment Variables:**
```bash
QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT=http://k12-signoz-otel-collector:4318
```

**System Property (in code):**
```java
System.getProperty("quarkus.otel.exporter.otlp.endpoint")
```

**Mapping Question:**
- `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT` (env var)
- `quarkus.otel.exporter.otlp.endpoint` (system property)
- Are these automatically mapped by Quarkus?

### Evidence Gap

**Missing:**
- Haven't verified what endpoint the handler is actually using
- Don't know if the system property is set
- No debug output showing the resolved endpoint
- No Quarkus documentation review

### Evidence Needed

1. **Check system properties at runtime:**
   ```java
   // Add to OpenTelemetryLogHandler constructor:
   System.out.println("OTEL Endpoint from system prop: " + 
       System.getProperty("quarkus.otel.exporter.otlp.endpoint", "NOT SET"));
   System.out.println("OTEL Endpoint from env var: " + 
       System.getenv("QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT"));
   ```

2. **Verify Quarkus mapping:**
   - Check Quarkus documentation for env var to system property mapping
   - Look for `ConfigMapping` in Quarkus source
   - Verify if OTLP properties are mapped

3. **Test different approaches:**
   - Try using System.setProperty() explicitly
   - Try reading from environment variable directly
   - Try using @ConfigProperty injection

### Investigation Commands

```bash
# Check what system properties are set
docker exec k12-backend jps -l
docker exec k12-backend jinfo <pid> | grep otel

# Check if Quarkus logs the resolved configuration
docker logs k12-backend 2>&1 | grep -i "otel.*endpoint"
```

---

## Unknown #5: Collector Receiving Any Data

### Question
Is the OTLP collector receiving ANY data from ANY source?

### What We Know

✅ **Confirmed:**
- Collector is running (uptime ~12 hours)
- OTLP receivers are started (gRPC :4317, HTTP :4318)
- Health checks passing
- SigNoz UI is accessible

❌ **Unknown:**
- Is the collector receiving data from other services?
- Are there other applications sending telemetry successfully?
- Is this a global collector issue or specific to k12-backend?
- Are the exporters working for other applications?

### Evidence Gap

**Missing:**
- Don't know if other services are successfully exporting
- Haven't checked if collector is working for ANY traces/metrics/logs
- No baseline verification of collector functionality

### Evidence Needed

1. **Check for any recent traces/metrics from any service:**
   ```sql
   -- Check if ANY traces exist (not just k12-backend)
   SELECT count() FROM signoz_traces.distributed_signoz_spans;
   
   -- Check if ANY metrics exist
   SELECT count() FROM signoz_metrics.time_series_v2;
   
   -- Check services with recent activity
   SELECT DISTINCT service_name 
   FROM signoz_logs.logs_v2 
   WHERE timestamp > now() - INTERVAL 1 DAY;
   ```

2. **Check collector receiver statistics:**
   - Are receivers processing requests?
   - Are there any ingestion errors?
   - What's the request rate?

3. **Verify collector functionality:**
   - Send test trace manually
   - Send test metrics manually
   - Verify collector receives them

### Investigation Commands

```sql
-- Check all services in ClickHouse
docker exec k12-clickhouse clickhouse-client --query "
  SELECT 
    count() as trace_count
  FROM signoz_traces.distributed_signoz_spans
"

-- Check for any metrics
docker exec k12-clickhouse clickhouse-client --query "
  SELECT 
    count() as metric_count
  FROM signoz_metrics.time_series_v2
"

-- Check recent logs from all services
docker exec k12-clickhouse clickhouse-client --query "
  SELECT 
    resources_string['service.name'] as service,
    count() as log_count,
    max(timestamp) as latest_log
  FROM signoz_logs.logs_v2
  GROUP BY service
  ORDER BY latest_log DESC
"
```

---

## Unknown #6: Port and Protocol Conflicts

### Question
Is there a port conflict or protocol version mismatch causing failures?

### What We Know

✅ **Confirmed:**
- Backend configured for HTTP/protobuf on :4318
- Collector listening on HTTP :4318 and gRPC :4317
- Log handler hardcoded for gRPC :4317

❌ **Unknown:**
- Is there a protocol version mismatch (HTTP/1.1 vs HTTP/2)?
- Is the collector expecting a specific content-type?
- Are there TLS/SSL negotiation issues?
- Is there a proxy or load balancer interfering?

### Evidence Gap

**Missing:**
- Don't know actual HTTP protocol being used
- Haven't verified HTTP headers
- No packet-level visibility

### Evidence Needed

1. **Capture HTTP traffic:**
   ```bash
   # Install tcpdump in container
   docker exec k12-backend apt-get update && apt-get install -y tcpdump
   
   # Capture traffic
   docker exec k12-backend tcpdump -i any port 4318 -A -w -
   ```

2. **Check HTTP version:**
   - Is Quarkus using HTTP/1.1 or HTTP/2?
   - Does the collector support both versions?
   - Is there a version negotiation failure?

3. **Verify content-type headers:**
   - What content-type is Quarkus sending?
   - What content-type does collector expect?
   - Is there a mismatch?

---

## Summary of Unknowns

| # | Unknown Category | Severity | Evidence Needed | Estimated Effort |
|---|-----------------|----------|-----------------|------------------|
| 1 | Trace export root cause | Critical | HTTP logs, collector logs | Medium |
| 2 | Scraping stopped | Critical | Collector state, health check | Low |
| 3 | Log handler called | High | Debug logs, code instrumentation | Low |
| 4 | Env var mapping | High | Runtime verification, docs review | Low |
| 5 | Collector receiving data | Medium | ClickHouse queries, test data | Low |
| 6 | Protocol conflicts | Medium | Packet capture, header inspection | Medium |

**Total Unknowns:** 6  
**Critical:** 2  
**High:** 2  
**Medium:** 2

All unknowns are investigable with targeted evidence gathering.
