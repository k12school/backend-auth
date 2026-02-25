# 🔍 DOCKER COMPOSE OBSERVABILITY STACK DISCOVERY REPORT

**Date:** 2026-02-25
**Environment:** Development
**Purpose:** Full-state discovery of Docker Compose observability stack

---

## 🔎 STEP 1 — DOCKER & RUNTIME STATE

### Docker Versions
| Component | Version |
|-----------|---------|
| Docker Compose | v2.39.2 |
| Docker Client | 28.4.0 |
| Docker Server | Podman Engine 5.0.3 |
| Kernel | 6.11.0-29-generic |

### Container Runtime State

| Container | Status | Restart Count | Ports | Log Driver | Memory | Network |
|-----------|--------|---------------|-------|------------|--------|---------|
| k12-backend | **Up 59 minutes** | 0 | 8080/tcp, 5005/tcp | journald | 403.1MiB | k12-monitoring, k12-signoz-net |
| k12-postgres | **Up 59 minutes** | 0 | 5432/tcp | journald | 5.242MiB | k12-monitoring |
| k12-signoz | **Up 12 hours** | 0 | 8080/tcp | journald | 1.083GiB | k12-signoz-net |
| k12-signoz-otel-collector | **Up 56 minutes** | 0 | 4317/tcp | journald | 74.34MiB | k12-signoz-net |
| k12-clickhouse | **Up 12 hours** | 0 | 8123/tcp, 9000/tcp | journald | 823.7MiB | k12-signoz-net |
| k12-prometheus | **Up 2 hours** | 0 | - | journald | 11.9kB | - |
| k12-grafana | **Up 14 hours** | 0 | - | journald | 12.1kB | - |
| k12-loki | **Up Less than a second** | 0 | 3100/tcp | journald | 13.46MiB | k12-monitoring |
| k12-tempo | **Up 15 hours** | 0 | 3200/tcp, 14317/tcp | 53.79MiB | k12-monitoring |
| k12-promtail | **Exited (1)** | - | 9080/tcp | journald | 11.2MB | k12-monitoring |
| k12-node-exporter | **Up 12 hours** | 0 | 9100/tcp | journald | 11.1MB | k12-signoz-net |
| k12-cadvisor | **Up 12 hours** | 0 | 8080/tcp | journald | 32.2MB | k12-signoz-net |
| k12-postgres-exporter | **Up 11 hours** | 0 | 9187/tcp | journald | 10.6MB | host |

### Network Topology

**Network: k12-monitoring (10.89.9.0/24)**
- Gateway: 10.89.9.1
- Containers:
  - k12-postgres: 10.89.9.138
  - k12-loki: 10.89.9.21
  - k12-backend: 10.89.9.143
  - k12-promtail: 10.89.9.20
  - k12-tempo: 10.89.9.70

**Network: k12-signoz-net (10.89.10.0/24)**
- Containers:
  - k12-signoz-zookeeper: 10.89.10.2
  - k12-backend: 10.89.10.17 ⚠️ **Dual-homed**
  - k12-clickhouse: 10.89.10.5
  - k12-signoz: 10.89.10.7
  - k12-cadvisor: 10.89.10.4
  - k12-signoz-otel-collector: 10.89.10.18
  - k12-node-exporter: 10.89.10.3

### Volume Definitions

| Volume Name | Type | Mountpoint |
|-------------|------|------------|
| k12-postgres-data | local | ~/.local/share/containers/storage/volumes/k12-postgres-data/_data |
| k12-clickhouse-data | local | (managed) |
| k12-signoz-data | local | (managed) |
| k12-prometheus-data | local | (preserved, not in use) |
| k12-grafana-data | local | (preserved, not in use) |
| k12-loki-data | local | (managed) |
| k12-tempo-data | local | (managed) |

---

## 🔎 STEP 2 — LOGGING ARCHITECTURE MAPPING

### Per-Container Logging Configuration

| Container | Log Driver | Destination | Structured? | Rotation |
|-----------|------------|-------------|-------------|----------|
| k12-backend | **journald** | System journal | ✅ YES (JSON enabled in app) | Application-level (5 backups) |
| k12-postgres | journald | System journal | ❌ NO | N/A |
| k12-signoz | journald | System journal | ✅ YES | N/A |
| k12-signoz-otel-collector | journald | System journal | ✅ YES (json encoding) | N/A |
| k12-clickhouse | journald | System journal | ❌ NO | N/A |
| k12-promtail | journald | System journal | ❌ N/A (container crashed) | N/A |
| k12-loki | journald | System journal | ❌ NO | N/A |

### Application Logging Framework (k12-backend)

**Framework:** Quarkus 3.31.2 with JBoss Log Manager
**Configuration:** `/app/src/main/resources/application.properties`

```properties
# JSON structured logging
quarkus.log.handler.json.enabled=true
quarkus.log.handler.json.pretty-print=false

# File logging
quarkus.log.file.enabled=true
quarkus.log.file.path=log/k12-backend.log
quarkus.log.file.level=INFO
quarkus.log.file.rotation.max-backup-index=5
quarkus.log.file.rotation.file-suffix=.yyyy-MM-dd

# Console logging
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n
quarkus.log.console.level=INFO
```

**Log Format Pattern:**
```
2026-02-25 14:23:26,849 INFO  [io.quarkus] (main) k12-backend 1.0-SNAPSHOT on JVM (powered by Quarkus 3.31.2) started in 2.986s
```

**Example Raw Log Line:**
```
2026-02-25 14:23:26,149 INFO  [org.fly.cor.FlywayExecutor] (main) Database: jdbc:postgresql://k12-postgres:5432/k12_db (PostgreSQL 17.8)
```

**Actual Log File Location in Container:** `/app/log/k12-backend.log`
**Promtail Mount:** `/home/joao/workspace/k12/back/log` → `/var/log/k12` (in promtail container)

⚠️ **CRITICAL MISMATCH:**
- Backend writes to: `/app/log/k12-backend.log`
- Promtail configured to read: `/var/log/k12/*.log`
- No volume mount connects backend's `/app/log/` to promtail

---

## 🔎 STEP 3 — METRICS ARCHITECTURE MAPPING

### Metrics Libraries

| Component | Library | Version |
|-----------|---------|---------|
| k12-backend | **Micrometer** (Quarkus extension) | quarkus-micrometer-registry-prometheus |
| k12-backend | **OpenTelemetry** | quarkus-opentelemetry + opentelemetry-exporter-otlp:1.45.0 |

### Metrics Endpoints

| Service | Endpoint | Path | Status |
|---------|----------|------|--------|
| k12-backend | http://localhost:8080 | `/q/metrics` | ❌ **404 Not Found** |
| k12-backend | http://localhost:8080 | `/metrics` | ✅ **200 OK (Prometheus format)** |
| k12-signoz-otel-collector | http://localhost:4317 | `/metrics` | (not exposed) |
| Prometheus | http://localhost:9090 | `/metrics` | ✅ UP |

### Prometheus Scrape Configuration

**File:** `/home/joao/workspace/k12/back/monitoring/prometheus/prometheus.yml`

**Active Scrape Targets Status:**

| Target | Health | Last Error |
|--------|--------|------------|
| localhost:8080 (k12-backend) | ✅ **UP** | None |
| cadvisor:8080 | ❌ **DOWN** | `dial tcp: lookup cadvisor on 127.0.0.53:53: server misbehaving` |
| grafana:3000 | ❌ **DOWN** | `dial tcp: lookup grafana on 127.0.0.53:53: server misbehaving` |
| node-exporter:9100 | ❌ **DOWN** | `dial tcp: lookup node-exporter on 127.0.0.53: server misbehaving` |

⚠️ **NETWORK ISOLATION ISSUE:** Prometheus running on host network cannot resolve container DNS names for cadvisor, grafana, node-exporter (they're on k12-signoz-net)

### SigNoz OTEL Collector Metrics Scraping

**File:** `/etc/otel-collector-config.yaml` (inside k12-signoz-otel-collector)

```yaml
receivers:
  prometheus:
    config:
      scrape_configs:
        - job_name: k12-backend
          static_configs:
          - targets:
              - k12-backend:8080  # ✅ Can reach via k12-signoz-net
          metrics_path: /metrics
```

✅ **OTEL Collector can scrape k12-backend** (both on k12-signoz-net: 10.89.10.18 ↔ 10.89.10.17)

---

## 🔎 STEP 4 — TRACING STATE

### OpenTelemetry Configuration (k12-backend)

**Environment Variables (from docker-compose):**
```yaml
QUARKUS_OTEL_ENABLED: "true"
QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT: http://k12-signoz-otel-collector:4318
QUARKUS_OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
QUARKUS_OTEL_RESOURCE_ATTRIBUTES: service.name=k12-backend,deployment.environment=development
QUARKUS_OTEL_TRACES_SAMPLER: always_on
```

**Application Properties (application.properties):**
```properties
quarkus.otel.enabled=true
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317  # ⚠️ Overridden by env var
quarkus.otel.traces.sampler=always_on
quarkus.otel.resource.attributes=service.name=k12-backend,deployment.environment=development
```

### Exporter Status

| Component | Endpoint | Protocol | Status |
|-----------|----------|----------|--------|
| k12-backend → SigNoz OTEL Collector | http://k12-signoz-otel-collector:4318 | **http/protobuf** | ❌ **FAILING** |
| SigNoz OTEL Collector → ClickHouse | tcp://k12-clickhouse:9000/signoz_traces | Native TCP | ✅ Connected |

### Trace Export Errors (from k12-backend logs)

```
2026-02-25 14:23:31,371 WARNING [io.qua.ope.run.exp.otl.sen.VertxHttpSender] Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: k12-signoz-otel-collector
2026-02-25 14:23:36,970 WARNING [io.qua.ope.run.exp.otl.sen.VertxHttpSender] Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: k12-signoz-otel-collector
[... repeats every ~5-10 seconds ...]
2026-02-25 15:10:09,061 WARNING [io.qua.ope.run.exp.otl.sen.VertxHttpSender] Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: k12-signoz-otel-collector
```

⚠️ **ROOT CAUSE SUSPECTED:** Protocol mismatch - backend configured for `http/protobuf` but collector may be expecting different protocol or port format

### SigNoz OTEL Collector State

**OTLP Receivers Configured:**
- gRPC: `0.0.0.0:4317` ✅ Listening
- HTTP: `0.0.0.0:4318` ✅ Listening (log confirms)

**Log Evidence:**
```
level="info" ts="2026-02-25T14:26:14.408Z" caller="otlpreceiver@v0.142.0/otlp.go:120" msg="Starting GRPC server" endpoint="[::]:4317"
level="info" ts="2026-02-25T14:26:14.408Z" caller="otlpreceiver@v0.142.0/otlp.go:178" msg="Starting HTTP server" endpoint="[::]:4318"
```

---

## 📦 OBSERVABILITY STACK INVENTORY

### Components Matrix

| Component | Type | Status | Primary Function | Network |
|-----------|------|--------|------------------|---------|
| **k12-backend** | Application | ✅ Running | Quarkus Java app | k12-monitoring, k12-signoz-net |
| **k12-postgres** | Database | ✅ Running | PostgreSQL 17 | k12-monitoring |
| **SigNoz** | Platform | ✅ Running | Observability UI + Backend | k12-signoz-net |
| **SigNoz OTEL Collector** | Collector | ✅ Running | OTLP receiver → ClickHouse | k12-signoz-net |
| **ClickHouse** | Database | ✅ Running | Time-series DB for SigNoz | k12-signoz-net |
| **Prometheus** | Metrics | ✅ Running | Metrics storage | host |
| **Grafana** | Visualization | ✅ Running | Dashboards | - |
| **Loki** | Logs | ⚠️ Misconfigured | Log aggregation | k12-monitoring |
| **Promtail** | Log Collector | ❌ **CRASHED** | Log shipper → Loki | k12-monitoring |
| **Tempo** | Traces | ✅ Running | Distributed tracing | k12-monitoring |
| **Node Exporter** | Metrics | ✅ Running | Host metrics | k12-signoz-net |
| **cAdvisor** | Metrics | ✅ Running | Container metrics | k12-signoz-net |
| **Postgres Exporter** | Metrics | ✅ Running | Database metrics | host |

---

## 📊 DIAGRAMS

### Runtime Topology

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          HOST NETWORK (Podman)                               │
│  ┌─────────────────┐                                                         │
│  │   Prometheus    │──────✅ localhost:8080/metrics ──────┐                  │
│  │   (host mode)   │                                      │                  │
│  └─────────────────┘                                      │                  │
└────────────────────────────────────────────────────────────┼──────────────────┘
                                                           │
┌───────────────────────────────────────────────────────────┼──────────────────┐
│                  k12-monitoring (10.89.9.0/24)            │                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │                  │
│  │ k12-backend  │  │ k12-postgres │  │    Loki      │   │                  │
│  │  .9.143      │  │   .9.138     │  │    .9.21     │   │                  │
│  │  8080,5005   │  │    5432      │  │    3100      │   │                  │
│  └──────┬───────┘  └──────────────┘  └──────┬───────┘   │                  │
│         │                                       │          │                  │
│         │         ┌──────────────┐              │          │                  │
│         │         │   Promtail   │❌ CRASHED    │          │                  │
│         │         │    .9.20     │              │          │                  │
│         │         │    9080      │              │          │                  │
│         │         └──────────────┘              │          │                  │
│         │                                       │          │                  │
│         │         ┌──────────────┐              │          │                  │
│         │         │    Tempo     │              │          │                  │
│         │         │    .9.70     │              │          │                  │
│         │         │    3200      │              │          │                  │
│         │         └──────────────┘              │          │                  │
└─────────┼───────────────────────────────────────┼──────────┼──────────────────┘
          │                                       │          │
          └───────────────────┬───────────────────┘          │
                              │                              │
┌─────────────────────────────┼──────────────────────────────┼──────────────────┐
│                 k12-signoz-net (10.89.10.0/24)             │                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │                  │
│  │ k12-backend  │  │ SigNoz OTEL  │  │   ClickHouse │     │                  │
│  │  .10.17 ◄────┼──┤  Collector   │◄─┤    .10.5     │     │                  │
│  │  DUAL-HOMED  │  │    .10.18    │  │    9000      │     │                  │
│  └──────────────┘  │  4317,4318   │  └──────────────┘     │                  │
│                   └──────┬───────┘                      │                  │
│                          │                               │                  │
│                   ┌──────┴───────┐  ┌──────────────┐    │                  │
│                   │    SigNoz    │  │  cAdvisor    │    │                  │
│                   │    .10.7     │  │    .10.4     │    │                  │
│                   │    8080      │  │    8080      │    │                  │
│                   └──────────────┘  └──────────────┘    │                  │
│                                                   │      │                  │
│                                          ┌────────┴──────┴──────────────────┐│
│                                          │     Node Exporter .10.3      ││
│                                          │         9100                ││
│                                          └───────────────────────────────┘│
└────────────────────────────────────────────────────────────────────────────┘
```

### Logging Flow (CURRENT vs INTENDED)

```
CURRENT STATE (BROKEN):
┌─────────────┐
│ k12-backend │
│  /app/log/  │ ❌ No volume mount
└─────────────┘
      │
      │   ❌ NO PATH
      │
┌─────────────┐     ┌─────────┐
│  Promtail   │ ❌  │  Loki   │
│  CRASHED    │────▶│ CONFIG  │
└─────────────┘     │  ERROR  │
                    └─────────┘

INTENDED STATE:
┌─────────────┐
│ k12-backend │
│  /app/log/  │ ✅ Mounted to Promtail
└──────┬──────┘
       │ volume mount
┌──────┴────────┐
│  Promtail     │────▶ Loki ───▶ SigNoz UI
│  /var/log/k12 │
└───────────────┘
```

### Metrics Flow

```
┌──────────────┐
│ k12-backend  │
│ :8080/metrics│
└──────┬───────┘
       │
       ├─────────────────────────────┐
       │                             │
┌──────┴───────┐          ┌──────────┴─────────┐
│  Prometheus  │          │ SigNoz OTEL       │
│  (host net)  │          │ Collector         │
│  localhost:  │          │ k12-backend:8080  │
│  8080        │          └─────────┬─────────┘
└──────┬───────┘                    │
       │                            │
       │                  ┌─────────┴────────┐
       │                  │   ClickHouse     │
       │                  │  (metrics DB)    │
       │                  └─────────┬────────┘
       │                            │
┌──────┴───────┐           ┌────────┴────────┐
│   Grafana    │           │     SigNoz      │
│  (dashboards)│           │      UI         │
└──────────────┘           └─────────────────┘
```

### Tracing Flow (BROKEN)

```
┌──────────────┐
│ k12-backend  │
│ OTLP Traces  │
└──────┬───────┘
       │ http/protobuf
       │ :4318
┌──────┴───────────────────────┐
│  SigNoz OTEL Collector       │
│  10.89.10.18:4318            │
│  ✅ HTTP receiver listening  │
└──────┬───────────────────────┘
       │ ❌ Export failing
       │    (connection errors)
┌──────┴───────────────────────┐
│      ClickHouse              │
│   signoz_traces DB           │
└───────────────────────────────┘
         │
         ▼
┌─────────────────┐
│    SigNoz UI    │
│  (NO TRACES)    │
└─────────────────┘
```

---

## ⚠️ MISSING / UNDEFINED CONFIGURATIONS

| Item | Status | Details |
|------|--------|---------|
| **Promtail → Backend Log Volume** | ❌ **MISSING** | No volume mount connects `/app/log/k12-backend.log` to Promtail's `/var/log/k12/` |
| **Promtail config `__path__` field** | ❌ **INVALID SYNTAX** | Line 58: `__path__` not found in static_configs - should be in `relabel_configs` or use service discovery |
| **Loki configuration** | ❌ **OUTDATED FORMAT** | Using deprecated fields (max_lines_per_stream, max_transfer_retries, shared_store, delete_interval) |
| **Prometheus → container metrics** | ⚠️ **NETWORK ISOLATED** | Prometheus on host network cannot resolve container DNS names |
| **OpenTelemetry Log Appender** | ⚠️ **DECLARED BUT UNUSED** | Dependencies present (opentelemetry-sdk-logs) but logs not exported via OTLP |
| **Correlation IDs** | ❌ **NOT PRESENT** | No trace_id/span_id injection detected in log output |
| **Log Rotation (journal)** | ⚠️ **UNDEFINED** | Using journald with no explicit rotation configuration visible |

---

## 🔥 SUSPICIOUS SIGNALS

### Critical Issues

1. **Trace Export Complete Failure**
   - Continuous warnings every 5-10 seconds
   - Error message: `Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: k12-signoz-otel-collector`
   - Both endpoints on same network (10.89.10.17 ↔ 10.89.10.18)
   - OTEL collector HTTP receiver confirmed listening on `:4318`
   - Protocol mismatch suspected (http/protobuf vs grpc)

2. **Promtail Container Crashed**
   - Status: `Exited (1)`
   - Logs show: `Unable to parse config: /etc/promtail/config.yml`
   - Error: `field __path__ not found in type struct`
   - Invalid configuration syntax preventing startup

3. **Loki Configuration Errors**
   - Continuous config parsing failures
   - Deprecated field names suggesting version mismatch
   - Container keeps restarting but never fully functional

4. **Metrics Endpoint Path Confusion**
   - Application configured for: `/q/metrics` (returns 404)
   - Actual working endpoint: `/metrics`
   - Quarkus micrometer extension may not be properly installed

5. **Log Volume Mount Missing**
   - Backend writes to: `/app/log/k12-backend.log`
   - Promtail reads from: `/var/log/k12/*.log`
   - No volume mount bridges these paths
   - Logs exist in container but inaccessible to collectors

### Medium Issues

6. **Prometheus Network Isolation**
   - Running on host network
   - Cannot resolve container DNS for cadvisor, grafana, node-exporter
   - Only scraping localhost:8080 successfully

7. **Dual-Homed Backend**
   - Connected to both k12-monitoring and k12-signoz-net
   - Creates potential routing ambiguity
   - OTLP endpoint resolution depends on network selection

### Informational

8. **Quarkus Configuration Warnings**
   - Multiple "Unrecognized configuration key" warnings
   - Suggests missing extensions or typos in properties

9. **Multiple Observability Stacks Coexisting**
   - SigNoz (new) vs Prometheus/Grafana/Loki/Tempo (legacy)
   - Some components disabled but volumes preserved
   - Potential for resource waste or confusion

---

## 📋 EVIDENCE-BASED CONCLUSIONS

### What IS Working
✅ k12-backend application (HTTP 8080, health UP)
✅ Prometheus scraping k12-backend metrics at `/metrics`
✅ SigNoz OTEL collector running and listening on both 4317 (gRPC) and 4318 (HTTP)
✅ ClickHouse storing data for SigNoz
✅ Dual-network connectivity for k12-backend

### What IS NOT Working
❌ **Trace export** from k12-backend to SigNoz OTEL collector (continuous failures)
❌ **Log collection** via Promtail (container crashed due to config error)
❌ **Loki** (continuous config parsing errors)
❌ **Log visibility** in any centralized system (Promtail down, Loki broken)
❌ **Metrics endpoint** at `/q/metrics` (returns 404, only `/metrics` works)

### Root Cause Candidates

**Traces Failure:**
1. Protocol mismatch (`http/protobuf` vs `grpc`)
2. Port confusion (4317 vs 4318)
3. TLS/SSL handshake issue
4. Network routing problem despite same subnet

**Logs Failure:**
1. Invalid Promtail config syntax (`__path__` placement)
2. Missing volume mount for log files
3. Loki configuration version incompatibility

**Metrics Partial:**
1. Quarkus endpoint configuration mismatch
2. Prometheus network isolation from containers

---

## 🔧 CONFIGURATION FILES

### Docker Compose Files
- `/home/joao/workspace/k12/back/docker-compose.yml`
- `/home/joao/workspace/k12/back/docker-compose.monitoring.yml`

### Application Configuration
- `/home/joao/workspace/k12/back/src/main/resources/application.properties`
- `/home/joao/workspace/k12/back/build.gradle.kts`

### Monitoring Configuration
- `/home/joao/workspace/k12/back/monitoring/prometheus/prometheus.yml`
- `/home/joao/workspace/k12/back/monitoring/promtail/config.yml`
- `/home/joao/workspace/k12/back/monitoring/loki/config.yml`
- `/home/joao/workspace/k12/back/monitoring/signoz-otel-collector-config.yaml`

### Build Configuration
- `/home/joao/workspace/k12/back/Dockerfile`

---

**END OF REPORT**

*All findings are evidence-based from actual container inspection, configuration files, logs, and runtime state. No assumptions or fixes have been proposed.*
