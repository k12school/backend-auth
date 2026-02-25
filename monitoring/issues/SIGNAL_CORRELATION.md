# Signal Correlation Matrix

**Generated:** 2026-02-25 15:25:39 -03:00

This matrix shows the flow of each telemetry signal from production through storage to UI visualization.

---

## Signal Flow Overview

```
┌─────────────────┐    Transport     ┌──────────────────┐    Storage     ┌─────────────┐
│  Producing      │  ──────────────>  │  Collector       │  ────────────>  │  ClickHouse  │
│  Component      │                  │                  │                 │             │
└─────────────────┘                  └──────────────────┘                 └─────────────┘
        │                                     │                                     │
        │                                     │                                     │
        v                                     v                                     v
    [Generated]                          [Received]                          [Persisted]
        │                                     │                                     │
        │                                     │                                     │
        └─────────────────>  UI  <─────────────────────────────────────────┘
                          (SigNoz)
```

---

## Complete Correlation Matrix

### Signal: Traces

| Stage | Component | Status | Details | Evidence |
|-------|-----------|--------|---------|----------|
| **Production** | k12-backend (Quarkus) | ⚠️ | Spans generated, export attempted | Error logs every 5-15s |
| **Transport** | HTTP/protobuf on :4318 | ❌ | Export failing | VertxHttpSender errors |
| **Network** | k12-signoz-net | ✅ | Connectivity confirmed | TCP connection works |
| **Collector** | k12-signoz-otel-collector | ❓ | No ingestion logs | No trace reception logs |
| **Storage** | ClickHouse signoz_traces | ❌ | 0 rows in database | `count() = 0` |
| **UI** | SigNoz Dashboard | ❌ | No data to display | Empty services list |

**Status:** ❌ **FAILED** - Production working, transport failing

---

### Signal: Metrics

| Stage | Component | Status | Details | Evidence |
|-------|-----------|--------|---------|----------|
| **Production** | k12-backend (Micrometer) | ✅ | Metrics endpoint responding | Prometheus format verified |
| **Transport** | HTTP scrape on :8080 | ❌ | Scraping stopped | Last scrape: 14:25:00 |
| **Network** | k12-signoz-net | ✅ | Connectivity confirmed | DNS resolution working |
| **Collector** | k12-signoz-otel-collector | ❌ | Scrape job added, no execution | Job added at 14:26:14 |
| **Storage** | ClickHouse signoz_metrics | ❌ | 0 rows in database | `count() = 0` |
| **UI** | SigNoz Dashboard | ❌ | No data to display | Empty metrics |

**Status:** ❌ **FAILED** - Production working, scraping stopped

---

### Signal: Logs

| Stage | Component | Status | Details | Evidence |
|-------|-----------|--------|---------|----------|
| **Production** | k12-backend (OpenTelemetryLogHandler) | ❓ | Handler registered, no export visible | Startup msg only |
| **Transport** | gRPC on :4317 (intended) | ❌ | Protocol mismatch | Handler uses gRPC, env says HTTP |
| **Network** | k12-signoz-net | ✅ | Connectivity confirmed | Both containers on network |
| **Collector** | k12-signoz-otel-collector | ❌ | Not receiving logs | Wrong port/protocol |
| **Storage** | ClickHouse signoz_logs | ⚠️ | 12 old rows only | All from dev mode, pre-Docker |
| **UI** | SigNoz Dashboard | ❌ | No new logs visible | Only old data showing |

**Status:** ❌ **FAILED** - Configuration mismatch blocking transport

---

## Detailed Breakdown by Layer

### Layer 1: Data Production

**Traces:**
- ✅ Quarkus OpenTelemetry generating spans
- ✅ Batch processor configured (1s delay)
- ✅ VertxHttpSender attempting exports
- ❌ Export failing with generic error

**Metrics:**
- ✅ Micrometer collecting JVM metrics
- ✅ Prometheus endpoint working at `/metrics`
- ✅ Metrics exposed in Prometheus format
- ✅ Endpoint accessible on port 8080

**Logs:**
- ✅ OpenTelemetryLogHandler registered at startup
- ❓ Log production in prod mode uncertain
- ❌ No export activity visible
- ❌ Configuration mismatch with handler

**Layer 1 Status:** ⚠️ **Partially Working** - Production confirmed, some issues

---

### Layer 2: Transport

**Traces (HTTP/protobuf :4318):**
- ❌ Request execution failing
- ✅ Network connectivity confirmed
- ✅ DNS resolution working
- ✅ HTTP endpoint accessible
- ❌ Unknown if reaching collector

**Metrics (HTTP scrape :8080):**
- ❌ Scraping not executing
- ✅ Target endpoint responding
- ✅ Network connectivity confirmed
- ❓ Scrape job added but not running

**Logs (gRPC :4317):**
- ❌ Wrong protocol configured
- ❌ Handler using default endpoint (localhost:4317)
- ❌ Should be HTTP :4318 per env vars
- ❌ Configuration source mismatch

**Layer 2 Status:** ❌ **FAILING** - Transport layer blocking all signals

---

### Layer 3: Collection

**OTLP Collector Status:**
- ✅ Collector running (uptime ~12 hours)
- ✅ gRPC receiver listening on `[::]:4317`
- ✅ HTTP receiver listening on `[::]:4318`
- ✅ Health checks passing
- ❌ No trace ingestion logs visible
- ❌ No metrics scraping logs (post-restart)
- ❌ No log ingestion logs visible

**Configuration:**
- ✅ Prometheus scrape job configured
- ✅ OTLP receivers enabled
- ✅ ClickHouse exporters configured
- ❓ Scraping not executing despite config

**Layer 3 Status:** ❌ **NOT RECEIVING** - Collector not getting data

---

### Layer 4: Storage

**ClickHouse Status:**
- ✅ Database running (k12-clickhouse)
- ✅ Tables exist for traces, metrics, logs
- ❌ Traces table: 0 rows
- ❌ Metrics table: 0 time series
- ⚠️ Logs table: 12 old rows (dev mode)

**Layer 4 Status:** ⚠️ **EMPTY** - No new data being stored

---

### Layer 5: Visualization

**SigNoz UI:**
- ✅ UI accessible at http://localhost:3301
- ❌ No services showing
- ❌ No traces visible
- ❌ No metrics visible
- ⚠️ Old logs visible (dev mode data)

**Layer 5 Status:** ❌ **NO DATA** - Cannot visualize what doesn't exist

---

## Cross-Cutting Issues

### Issue A: Network Connectivity

| Component | Can Reach Other? | Evidence |
|-----------|-----------------|----------|
| Backend → Collector (DNS) | ✅ Yes | `getent hosts k12-backend` resolves |
| Backend → Collector (TCP) | ✅ Yes | Connection to :4317 succeeds |
| Backend → Collector (HTTP) | ✅ Yes | GET /v1/traces returns 405 |
| Collector → Backend (DNS) | ✅ Yes | `getent hosts k12-backend` resolves |
| Collector → Backend (HTTP) | ❓ Unknown | Scraping not executing |

**Conclusion:** Network is NOT the root cause

---

### Issue B: Configuration Alignment

| Component | Protocol | Port | Config Source | Correct? |
|-----------|----------|------|---------------|----------|
| Trace exporter (env) | http/protobuf | 4318 | Environment variable | ✅ |
| Trace exporter (actual) | http/protobuf | 4318 | VertxHttpSender logs | ✅ |
| Log handler (code) | gRPC | 4317 | Hardcoded | ❌ |
| Log handler (env) | http/protobuf | 4318 | Environment variable | ✅ |
| Metrics target | HTTP | 8080 | Prometheus scrape config | ✅ |
| Metrics scrape | - | - | Not executing | ❌ |

**Conclusion:** Log handler misconfigured

---

### Issue C: Timing Correlation

| Time | Event | Impact |
|------|--------|--------|
| 14:23:23 | Backend container started | Application available |
| 14:23:26 | Backend fully started | Logs show "OpenTelemetryLogHandler registered" |
| 14:23:30 | First trace export errors | Export attempts begin |
| 14:25:00 | Last successful metrics scrape | Scraping still working |
| 14:26:14 | Collector restarted | Scrape job re-added |
| 14:26:14+ | No more scrape attempts | Scraping stopped |
| 15:10:09 | Last trace export error seen | Export still failing |

**Conclusion:** Collector restart correlated with scraping stoppage

---

## Summary Table

| Signal | Producing | Transport | Collector | Storage | UI | Overall |
|--------|-----------|-----------|-----------|---------|-----|---------|
| **Traces** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Metrics** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Logs** | ⚠️ | ❌ | ❌ | ⚠️ | ❌ | ❌ |

**Legend:**
- ✅ Working correctly
- ⚠️ Partially working or old data only
- ❌ Not working

**Overall Observability Status:** **DOWN** (0% data flow from production to UI)
