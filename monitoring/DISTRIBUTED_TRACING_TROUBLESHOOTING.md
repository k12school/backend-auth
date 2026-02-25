# 🔍 Distributed Tracing Issue - Comprehensive Troubleshooting Report

**寻求帮助：Quarkus 3.31.2 OpenTelemetry traces not exporting to OTel Collector**

**Asking for help:** Quarkus 3.31.2 + OpenTelemetry traces not being exported despite agent loading successfully

---

## 📋 Table of Contents
1. [Current State](#current-state)
2. [Infrastructure Overview](#infrastructure-overview)
3. [All Attempts Made](#all-attempts-made)
4. [Configuration Files](#configuration-files)
5. [Logs and Error Messages](#logs-and-error-messages)
6. [Environment Details](#environment-details)
7. [What Works](#what-works)
8. [What Doesn't Work](#what-doesnt-work)

---

## Current State

### Status Summary
- ✅ **Tempo is running** on `localhost:3200` - ready to receive traces
- ✅ **OTel Collector is running** on `localhost:4317` - debug exporter active
- ✅ **OpenTelemetry Java Agent is loaded** - confirmed in logs
- ✅ **Quarkus is running** - application responding to requests
- ❌ **Traces are NOT being exported** - no traces in collector or Tempo

### Symptoms
1. Quarkus creates trace IDs internally (visible in logs with `sampled=false`)
2. OpenTelemetry Java Agent loads successfully: `opentelemetry-javaagent - version: 2.10.0`
3. OTel Collector shows "Everything is ready" but receives NO trace data
4. No errors in any logs - traces simply don't flow

---

## Infrastructure Overview

### Docker Compose Services
```yaml
# All using host networking except where noted
services:
  prometheus:      localhost:9090  # network_mode: host
  grafana:         localhost:3000  # bridge network
  loki:            localhost:3100  # bridge network
  promtail:        localhost:9080  # bridge network
  otel-collector:  localhost:4317  # network_mode: host ⚠️
  tempo:           localhost:3200  # bridge network
  postgres-exporter: localhost:9187 # network_mode: host
  uptime-kuma:     localhost:3001  # network_mode: host
  node-exporter:   localhost:9100  # bridge network
  cadvisor:        localhost:8081  # bridge network
```

### Network Architecture
- **Quarkus**: Running on host (not containerized) on `0.0.0.0:8080`
- **OTel Collector**: Using `network_mode: host` to receive from Quarkus
- **Tempo**: On bridge network, accessible from collector

---

## All Attempts Made

### Attempt 1: Quarkus OpenTelemetry Extension ❌

**Configuration in `application.properties`:**
```properties
quarkus.opentelemetry.enabled=true
quarkus.opentelemetry.tracer.exporter.otlp.endpoint=http://localhost:4317
quarkus.opentelemetry.tracer.sampler=on
quarkus.opentelemetry.tracer.sampler.ratio=1.0
quarkus.opentelemetry.tracer.exporter.otlp.protocol=grpc
quarkus.otel.instrument.jdbc.enabled=true
```

**build.gradle.kts:**
```gradle
implementation("io.quarkus:quarkus-opentelemetry")
```

**Result:**
```
WARN  Unrecognized configuration key "quarkus.opentelemetry.tracer.sampler" was provided
WARN  Unrecognized configuration key "quarkus.opentelemetry.tracer.exporter.otlp.endpoint" was provided
WARN  Unrecognized configuration key "quarkus.opentelemetry.enabled" was provided
```

**Quarkus logs showed:**
```
sampled=false (on all traces)
```

**Conclusion:** Configuration properties not recognized in Quarkus 3.31.2

---

### Attempt 2: Changed Sampler Configuration ❌

**Tried different sampler values:**
```properties
# Attempt 2a:
quarkus.opentelemetry.tracer.sampler=on
# Result: sampled=false

# Attempt 2b:
quarkus.opentelemetry.tracer.sampler=always_on
# Result: sampled=false

# Attempt 2c:
quarkus.opentelemetry.tracer.sampler=ratio
quarkus.opentelemetry.tracer.sampler.ratio=1.0
# Result: sampled=false
```

**All variations resulted in `sampled=false`**

---

### Attempt 3: Environment Variables ❌

**Set environment variables before starting Quarkus:**
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
export OTEL_SERVICE_NAME=k12-backend
export OTEL_TRACES_SAMPLER=always_on
export OTEL_TRACES_SAMPLER_ARG=1.0

./gradlew quarkusDev
```

**Result:** Same warnings, `sampled=false`

---

### Attempt 4: OpenTelemetry Java Agent (relative path) ❌

**Downloaded agent:**
```bash
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar
```

**Created startup script:**
```bash
#!/bin/bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
export OTEL_SERVICE_NAME=k12-backend
export OTEL_TRACES_SAMPLER=traceidratio
export OTEL_TRACES_SAMPLER_ARG=1.0

AGENT_JAR="./monitoring/otel-agent/opentelemetry-javaagent.jar"
export JAVA_TOOL_OPTIONS="-javaagent:$AGENT_JAR"

./gradlew quarkusDev
```

**Error:**
```
Error opening zip file or JAR manifest missing : ./monitoring/otel-agent/opentelemetry-javaagent.jar
Error occurred during initialization of VM
agent library failed Agent_OnLoad: instrument
```

**Issue:** Relative path doesn't work with Gradle worker daemons

---

### Attempt 5: OpenTelemetry Java Agent (absolute path) ⚠️

**Fixed startup script with absolute path:**
```bash
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AGENT_JAR="$SCRIPT_DIR/monitoring/otel-agent/opentelemetry-javaagent.jar"

export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
export OTEL_SERVICE_NAME=k12-backend
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none
export OTEL_TRACES_SAMPLER=traceidratio
export OTEL_TRACES_SAMPLER_ARG=1.0
export OTEL_RESOURCE_ATTRIBUTES=service.name=k12-backend,deployment.environment=development

export JAVA_TOOL_OPTIONS="-javaagent:$AGENT_JAR"

./gradlew quarkusDev
```

**Startup logs showed:**
```
[otel.javaagent 2026-02-24 20:01:05:528 -0300] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.10.0
```

**Quarkus started successfully:**
```
k12-backend 1.0-SNAPSHOT on JVM (powered by Quarkus 3.31.2) started in 18.784ms. Listening on: http://0.0.0.0:8080
```

**Installed features (notice NO opentelemetry):**
```
[agroal, cdi, compose, flyway, hibernate-validator, jdbc-postgresql, micrometer, narayana-jta, resteasy, resteasy-jackson, security, smallrye-context-propagation, smallrye-health, smallrye-jwt, smallrye-openapi, swagger-ui, vertx]
```

**Generated traffic:**
```bash
for i in {1..30}; do curl -s http://localhost:8080/q/health > /dev/null; done
```

**Checked OTel Collector:**
```bash
docker logs k12-otel-collector --since 30s
# Result: Only "Everything is ready" message, NO trace data
```

**Result:** Agent loads, Quarkus runs, but NO traces exported

---

### Attempt 6: Disabled Quarkus OTel Extension ❌

**Removed from build.gradle.kts:**
```gradle
// Commented out:
// implementation("io.quarkus:quarkus-opentelemetry")
```

**Set in application.properties:**
```properties
quarkus.opentelemetry.enabled=false
```

**Result:** Same - agent loads but no traces export

---

### Attempt 7: Modified OTel Collector Configuration ❌

**Initial config (caused crash):**
```yaml
exporters:
  logging:
    loglevel: debug
```

**Error:**
```
the logging exporter has been deprecated, use the debug exporter instead
```

**Fixed config:**
```yaml
exporters:
  debug:
    verbosity: detailed
```

**Result:** Collector starts successfully, but still receives no traces

---

### Attempt 8: Tried Different OTLP Endpoints ❌

**Tried:**
- `http://localhost:4317` (current)
- `http://127.0.0.1:4317`
- `http://host.docker.internal:4317`

**Result:** No change - traces not exporting

---

### Attempt 9: Added Agent Debug Logging ❌

**Tried adding to startup script:**
```bash
export OTEL_JAVAAGENT_DEBUG=true
export OTEL_LOG_LEVEL=DEBUG
```

**Result:** No additional debug output appeared

---

## Configuration Files

### 1. build.gradle.kts (Current State)
```gradle
plugins {
    id("java")
    id("io.quarkus") version "3.31.2"
    // ...
}

dependencies {
    implementation(platform("io.quarkus:quarkus-bom:3.31.2"))

    // Quarkus dependencies
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-resteasy")
    implementation("io.quarkus:quarkus-resteasy-jackson")
    implementation("io.quarkus:quarkus-jackson")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    // OpenTelemetry disabled - using Java Agent instead
    // implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.quarkus:quarkus-agroal")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    // ...
}
```

### 2. application.properties (Relevant Sections)
```properties
# HTTP configuration
quarkus.http.host=0.0.0.0
%test.quarkus.http.port=8080

# OpenTelemetry - DISABLED (using Java Agent instead)
quarkus.opentelemetry.enabled=false

# OTel logging
quarkus.log.category."io.opentelemetry".level=INFO

# Application name
quarkus.application.name=k12-backend
```

### 3. docker-compose.monitoring.yml (Relevant Sections)
```yaml
services:
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.119.0
    container_name: k12-otel-collector
    command: --config=/etc/otelcol-contrib/config.yaml
    volumes:
      - ./monitoring/otel-collector/config.yaml:/etc/otelcol-contrib/config.yaml:ro
    ports:
      - "4317:4317"   # OTLP gRPC receiver
      - "4318:4318"   # OTLP HTTP receiver
    network_mode: host  # ⚠️ Using host network
    restart: unless-stopped

  tempo:
    image: grafana/tempo:2.6.1
    container_name: k12-tempo
    command: -config.file=/etc/tempo.yaml
    volumes:
      - ./monitoring/tempo/tempo.yaml:/etc/tempo.yaml:ro
      - tempo-data:/tmp/tempo
    ports:
      - "3200:3200"   # Tempo UI
      - "9411:9411"   # Zipkin
    restart: unless-stopped
    networks:
      - monitoring
```

### 4. otel-collector/config.yaml (Current)
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317

processors:
  batch:

exporters:
  debug:
    verbosity: detailed

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [debug]
```

### 5. tempo/tempo.yaml (Current)
```yaml
server:
  http_listen_address: 0.0.0.0
  http_listen_port: 3200

distributor:
  receivers:
    otlp:
      protocols:
        grpc:
        http:

storage:
  trace:
    backend: local
    wal:
      path: /tmp/tempo/wal
    local:
      path: /tmp/tempo/blocks
```

### 6. start-with-otel-final.sh (Current Startup Script)
```bash
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AGENT_JAR="$SCRIPT_DIR/monitoring/otel-agent/opentelemetry-javaagent.jar"

# OpenTelemetry Configuration
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
export OTEL_SERVICE_NAME=k12-backend
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none
export OTEL_TRACES_SAMPLER=traceidratio
export OTEL_TRACES_SAMPLER_ARG=1.0
export OTEL_RESOURCE_ATTRIBUTES=service.name=k12-backend,deployment.environment=development

# Use absolute path for JAVA_TOOL_OPTIONS
export JAVA_TOOL_OPTIONS="-javaagent:$AGENT_JAR"

echo "Starting Quarkus with OpenTelemetry Java Agent..."
echo "Agent: $AGENT_JAR"
echo "OTLP Endpoint: $OTEL_EXPORTER_OTLP_ENDPOINT"
echo ""

./gradlew quarkusDev
```

---

## Logs and Error Messages

### Successful Agent Loading Log
```
[otel.javaagent 2026-02-24 20:01:05:528 -0300] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.10.0
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

### Quarkus Startup Log (with warnings)
```
2026-02-24 20:01:30,558 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.smallrye-openapi.inherit-interfaces" was provided
2026-02-24 20:01:30,558 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.http.cors" was provided
2026-02-24 20:01:30,559 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.micrometer.binder.jdbc.enabled" was provided
2026-02-24 20:01:30,559 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.opentelemetry.enabled" was provided
```

### Quarkus Successfully Started
```
2026-02-24 20:01:33,873 INFO  [io.quarkus] (Quarkus Main Thread) k12-backend 1.0-SNAPSHOT on JVM (powered by Quarkus 3.31.2) started in 18.784ms. Listening on: http://0.0.0.0:8080
2026-02-24 20:01:33,874 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2026-02-24 20:01:33,874 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [agroal, cdi, compose, flyway, hibernate-validator, jdbc-postgresql, micrometer, narayana-jta, resteasy, resteasy-jackson, security, smallrye-context-propagation, smallrye-health, smallrye-jwt, smallrye-openapi, swagger-ui, vertx]
```

### OTel Collector Startup Log
```
2026-02-24T22:45:33.675Z	info	service@v0.119.0/service.go:252	Starting otelcol-contrib...	{"Version": "0.119.0", "NumCPU": 28}
2026-02-24T22:45:33.675Z	info	extensions/extensions.go:39	Starting extensions...
2026-02-24T22:45:33.675Z	info	otlpreceiver@v0.119.0/otlp.go:112	Starting GRPC server	{"kind": "receiver", "name": "otlp", "data_type": "metrics", "endpoint": "0.0.0.0:4317"}
2026-02-24T22:45:33.675Z	info	service@v0.119.0/service.go:275	Everything is ready. Begin running and processing data.
```

### After Generating Traffic - NO TRACE DATA
```bash
$ docker logs k12-otel-collector --since 30s
# Only shows "Everything is ready" message
# NO trace/span data appears
```

---

## Environment Details

### System Information
```bash
$ uname -a
Linux <hostname> 6.11.0-29-generic #202411151232 UTC 2024 SMP x86_64 x86_64 x86_64 GNU/Linux

$ java -version
openjdk 25.0.2 2025-01-21
OpenJDK Runtime Environment (build 25.0.2+7)
OpenJDK 64-Bit Server VM (build 25.0.2+7, mixed mode, sharing)

$ ./gradlew --version
Gradle 9.3.1

$ java -jar monitoring/otel-agent/opentelemetry-javaagent.jar --version
OpenTelemetry Java Agent version: 2.10.0
```

### Docker/Podman
```bash
$ docker --version
Docker version 5.0.3
# (Actually using Podman as Docker compatible)
```

### Network Testing
```bash
# From host, can reach collector
$ curl -v http://localhost:4317
# Returns empty (expected for gRPC endpoint)

# OTel Collector is listening
$ netstat -tuln | grep 4317
tcp        0      0 0.0.0.0:4317            0.0.0.0:*               LISTEN

# Quarkus is responding
$ curl http://localhost:8080/q/health
{"status":"UP",...}
```

### Quarkus Details
- **Version:** 3.31.2
- **Build Tool:** Gradle 9.3.1
- **Java:** OpenJDK 25.0.2
- **Mode:** Dev mode (`quarkusDev`)

---

## What Works

### ✅ Metrics Collection
```bash
$ curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.job=="k12-backend") | .health'
"up"
```

Prometheus is successfully scraping:
- JVM metrics (heap, GC, threads)
- HTTP metrics (request rate, latency)
- Database metrics (via postgres-exporter)

### ✅ Log Aggregation
Loki is receiving logs from Promtail. Full-text search works in Grafana.

### ✅ Monitoring Stack
All components are running and accessible:
- Prometheus: http://localhost:9090 ✅
- Grafana: http://localhost:3000 ✅
- Loki: http://localhost:3100 ✅
- Tempo: http://localhost:3200 ✅
- OTel Collector: http://localhost:4317 ✅
- Postgres Exporter: http://localhost:9187/metrics ✅
- Uptime Kuma: http://localhost:3001 ✅

### ✅ Java Agent Loading
The OpenTelemetry Java Agent is confirmed to load:
```
[otel.javaagent 2026-02-24 20:01:05:528 -0300] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.10.0
```

---

## What Doesn't Work

### ❌ Trace Export from Quarkus

**Expected behavior:**
- Traces should be exported to `http://localhost:4317`
- OTel Collector should receive and log them via debug exporter
- Tempo should receive traces from collector

**Actual behavior:**
- NO traces appear in OTel Collector logs
- NO traces appear in Tempo UI
- NO error messages anywhere
- Application logs show normal operation
- Agent loads but appears to do nothing

**Observed in earlier attempts (with Quarkus OTel extension):**
```
sampled=false (on all internal trace creation)
```

---

## Specific Questions

1. **Environment Variable Propagation:** Why aren't the `OTEL_*` environment variables (set in the bash script that calls Gradle) propagating to the forked Quarkus dev mode process?

2. **Java Agent Compatibility:** Is the OpenTelemetry Java Agent v2.10.0 compatible with Quarkus 3.31.2 in dev mode?

3. **Networking:** Could the `network_mode: host` on the OTel Collector be causing issues? The collector should be accessible on `localhost:4317` from the host machine where Quarkus runs.

4. **Batching:** Is it possible traces are being batched but never sent? The agent defaults to a 5-second batch timeout, but even after generating many requests over minutes, no traces appear.

5. **Alternative Approaches:** What else can we try? Should we:
   - Build a production jar and run it directly (bypassing Gradle)?
   - Use a different OTLP endpoint format?
   - Add additional agent configuration?

---

## Reproduction Steps

1. Clone the repository (not provided, but using Quarkus 3.31.2)

2. Start monitoring stack:
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
   ```

3. Verify services are running:
   ```bash
   curl http://localhost:4317  # OTel Collector
   curl http://localhost:3200  # Tempo
   ```

4. Download OpenTelemetry Java Agent:
   ```bash
   wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar -O monitoring/otel-agent/opentelemetry-javaagent.jar
   ```

5. Start Quarkus with agent:
   ```bash
   ./start-with-otel-final.sh
   ```

6. Generate traffic:
   ```bash
   for i in {1..30}; do curl -s http://localhost:8080/q/health > /dev/null; done
   ```

7. Check OTel Collector:
   ```bash
   docker logs k12-otel-collector --since 30s
   ```

**Expected:** Traces appear in collector logs
**Actual:** Only "Everything is ready" message, no trace data

---

## Additional Context

### Why We Need Distributed Tracing
We want to trace requests through our multi-tenant application to:
- Debug performance issues
- Understand database query patterns
- Track request latency across layers
- Identify bottlenecks in JWT authentication and tenant resolution

### What We've Already Tried (from forums)
- ✅ Disabled Quarkus OpenTelemetry extension
- ✅ Used absolute path for javaagent
- ✅ Verified collector is listening on `0.0.0.0:4317`
- ✅ Generated significant traffic (100+ requests)
- ✅ Waited for batch timeout (5+ seconds)
- ✅ Checked firewall rules (all local)
- ❌ Haven't tried production jar (only dev mode)

---

## Request for Help

Please help us understand:

1. **Why is the OpenTelemetry Java Agent not exporting traces?**
2. **Are there known compatibility issues between Quarkus 3.31.2 and OTel Java Agent 2.10.0?**
3. **What configuration or approach are we missing?**
4. **How can we debug whether the agent is even attempting to export?**
5. **Should we try a completely different approach?**

Thank you for your help! 🙏

---

## Contact Information for Follow-up

- **Quarkus Version:** 3.31.2
- **Java Agent Version:** 2.10.0
- **OTel Collector Version:** 0.119.0
- **Tempo Version:** 2.6.1
- **Java Version:** OpenJDK 25.0.2
- **OS:** Linux (Ubuntu) with Podman/Docker

---

## Additional Files Available

If needed, I can provide:
- Full `build.gradle.kts`
- Complete `application.properties`
- All Docker Compose files
- OTel Collector and Tempo configurations
- Complete startup logs
- JVM process details
- Network configuration dump

Let me know what additional information would be helpful!
