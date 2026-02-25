# HELP: Quarkus 3.31.2 + OpenTelemetry - Traces not exporting despite agent loading

## TL;DR
OpenTelemetry Java Agent loads successfully in Quarkus 3.31.2, but traces are NOT being exported to OTel Collector. No errors, just silence. Need help debugging why.

---

## Current State
- ✅ Tempo running on localhost:3200
- ✅ OTel Collector running on localhost:4317
- ✅ OpenTelemetry Java Agent 2.10.0 loaded and running
- ✅ Quarkus 3.31.2 application responding on localhost:8080
- ❌ NO traces in collector - completely empty

---

## Environment
```bash
OS: Linux (Ubuntu) with Podman/Docker
Java: OpenJDK 25.0.2
Gradle: 9.3.1
Quarkus: 3.31.2
OTel Java Agent: 2.10.0
OTel Collector: 0.119.0
Tempo: 2.6.1
```

---

## Infrastructure

All services in Docker Compose:
```yaml
otel-collector:
  image: otel/opentelemetry-collector-contrib:0.119.0
  network_mode: host  # to receive from host machine
  ports:
    - "4317:4317"   # OTLP gRPC

tempo:
  image: grafana/tempo:2.6.1
  ports:
    - "3200:3200"   # Tempo UI
```

Quarkus runs on HOST (not containerized): `0.0.0.0:8080`

---

## What We Tried

### Attempt 1: Quarkus OpenTelemetry Extension
**build.gradle.kts:**
```gradle
implementation("io.quarkus:quarkus-opentelemetry")
```

**application.properties:**
```properties
quarkus.opentelemetry.enabled=true
quarkus.opentelemetry.tracer.exporter.otlp.endpoint=http://localhost:4317
quarkus.opentelemetry.tracer.sampler=on
quarkus.opentelemetry.tracer.sampler.ratio=1.0
```

**Startup Warnings:**
```
WARN  Unrecognized configuration key "quarkus.opentelemetry.tracer.sampler" was provided
WARN  Unrecognized configuration key "quarkus.opentelemetry.tracer.exporter.otlp.endpoint" was provided
```

**Result:** All traces show `sampled=false`, nothing exported

---

### Attempt 2: OpenTelemetry Java Agent

**Downloaded agent:**
```bash
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar
```

**Startup script (start-with-otel-final.sh):**
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

**Startup logs show agent loads:**
```
[otel.javaagent 2026-02-24 20:01:05:528 -0300] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.10.0
```

**Quarkus starts successfully:**
```
2026-02-24 20:01:33,873 INFO  [io.quarkus] (Quarkus Main Thread) k12-backend 1.0-SNAPSHOT on JVM (powered by Quarkus 3.31.2) started in 18.784ms. Listening on: http://0.0.0.0:8080
Installed features: [agroal, cdi, compose, flyway, hibernate-validator, jdbc-postgresql, micrometer, narayana-jta, resteasy, resteasy-jackson, security, smallrye-context-propagation, smallrye-health, smallrye-jwt, smallrye-openapi, swagger-ui, vertx]
```

**Note:** `opentelemetry` is NOT in installed features (extension disabled)

**Generated traffic:**
```bash
for i in {1..50}; do curl -s http://localhost:8080/q/health > /dev/null; done
```

**Checked collector:**
```bash
docker logs k12-otel-collector --since 60s
```

**Result:** Only "Everything is ready" message, ZERO trace data

---

### Attempt 3: Modified OTel Collector Config

**Initial config caused error:**
```yaml
exporters:
  logging:
    loglevel: debug
```
Error: "the logging exporter has been deprecated, use the debug exporter instead"

**Fixed config:**
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

**Collector logs:**
```
2026-02-24T22:45:33.675Z	info	service@v0.119.0/service.go:275	Everything is ready. Begin running and processing data.
```

**Result:** Collector running fine, but receives NO traces

---

### Attempt 4: Environment Variables Not Propagating?

Tried setting environment variables directly:
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_SERVICE_NAME=k12-backend
export OTEL_TRACES_SAMPLER=always_on
./gradlew quarkusDev
```

**Result:** Same - agent loads but no traces

---

### Attempt 5: Removed Quarkus OTel Extension Completely

**build.gradle.kts:**
```gradle
// implementation("io.quarkus:quarkus-opentelemetry")  # DISABLED
```

**application.properties:**
```properties
quarkus.opentelemetry.enabled=false
```

**Result:** No change - Java agent loads but exports nothing

---

## Current Configuration Files

### otel-collector/config.yaml
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

### tempo/tempo.yaml
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

### application.properties (relevant parts)
```properties
quarkus.application.name=k12-backend
quarkus.http.host=0.0.0.0
quarkus.opentelemetry.enabled=false
quarkus.log.category."io.opentelemetry".level=INFO
```

### build.gradle.kts (relevant parts)
```gradle
dependencies {
    implementation(platform("io.quarkus:quarkus-bom:3.31.2"))

    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-resteasy")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    // implementation("io.quarkus:quarkus-opentelemetry")  # DISABLED - using Java agent

    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    // ... other dependencies
}
```

---

## Full Startup Logs

### Agent Loading:
```
[otel.javaagent 2026-02-24 20:01:05:528 -0300] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.10.0
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

### Quarkus Startup:
```
2026-02-24 20:01:33,873 INFO  [io.quarkus] (Quarkus Main Thread) k12-backend 1.0-SNAPSHOT on JVM (powered by Quarkus 3.31.2) started in 18.784ms. Listening on: http://0.0.0.0:8080
2026-02-24 20:01:33,874 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2026-02-24 20:01:33,874 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [agroal, cdi, compose, flyway, hibernate-validator, jdbc-postgresql, micrometer, narayana-jta, resteasy, resteasy-jackson, security, smallrye-context-propagation, smallrye-health, smallrye-jwt, smallrye-openapi, swagger-ui, vertx]
```

### Collector Startup:
```
2026-02-24T22:45:33.675Z	info	service@v0.119.0/service.go:252	Starting otelcol-contrib...	{"Version": "0.119.0", "NumCPU": 28}
2026-02-24T22:45:33.675Z	info	extensions/extensions.go:39	Starting extensions...
2026-02-24T22:45:33.675Z	info	otlpreceiver@v0.119.0/otlp.go:112	Starting GRPC server	{"kind": "receiver", "name": "otlp", "data_type": "metrics", "endpoint": "0.0.0.0:4317"}
2026-02-24T22:45:33.675Z	info	service@v0.119.0/service.go:275	Everything is ready. Begin running and processing data.
```

---

## Network Verification

```bash
# Collector is listening
$ netstat -tuln | grep 4317
tcp        0      0 0.0.0.0:4317            0.0.0.0:*               LISTEN

# Quarkus is responding
$ curl http://localhost:8080/q/health
{"status":"UP","checks":[...]}

# From Quarkus, can we reach collector?
# (No errors connecting, but no traces sent either)
```

---

## The Mystery

1. **Agent loads successfully** - confirmed in logs
2. **Quarkus runs normally** - application works perfectly
3. **Collector is ready** - listening on 0.0.0.0:4317
4. **Traffic generated** - 50+ health check requests
5. **Wait time sufficient** - 60+ seconds for batch export
6. **NO errors anywhere** - completely silent failure
7. **NO traces appear** - collector receives nothing

---

## Questions

1. **Environment Variables:** Why aren't the `OTEL_*` environment variables propagating from the bash script to the forked Quarkus dev mode process?

2. **Agent Compatibility:** Is OpenTelemetry Java Agent 2.10.0 compatible with Quarkus 3.31.2 in dev mode?

3. **Debugging:** How can we verify the agent is even attempting to export? No logs appear.

4. **Batching:** Could traces be stuck in a batch buffer? We've waited minutes.

5. **Networking:** Could `network_mode: host` on the collector be causing issues? Quarkus runs on the host machine.

6. **Alternative:** Should we try building a production jar instead of using dev mode?

---

## What Else Should We Try?

We haven't tried:
- Building production jar and running directly (only dev mode)
- Adding `OTEL_JAVAAGENT_DEBUG=true` (tried, no additional output)
- Using HTTP instead of gRPC protocol
- Adding explicit exporter configuration
- Different agent versions

---

## What Works (for context)

✅ **Prometheus** - Successfully scraping metrics from Quarkus
✅ **Grafana** - 4 dashboards working with metrics
✅ **Loki** - Log aggregation working
✅ **Postgres Exporter** - 284 database metrics
✅ **Uptime Kuma** - Service monitoring

The monitoring stack works perfectly - ONLY distributed traces are missing.

---

## Reproduction Steps

1. Start monitoring: `docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d`
2. Download agent: `wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar`
3. Run: `./start-with-otel-final.sh` (script above)
4. Generate traffic: `for i in {1..50}; do curl http://localhost:8080/q/health; done`
5. Check collector: `docker logs k12-otel-collector --since 30s`
6. Expect traces, get nothing

---

## Help Needed

Please help us understand:

1. Why is the agent not exporting traces?
2. Are there known Quarkus 3.31.2 + OTel Agent 2.10.0 compatibility issues?
3. What configuration are we missing?
4. How can we debug this silent failure?
5. What else should we try?

Thank you! 🙏

---

## Additional Info Available

I can provide:
- Complete build.gradle.kts
- Full application.properties
- All Docker Compose files
- JVM process details
- Thread dumps
- Network dumps
- Any specific logs needed

Just let me know what would help!
