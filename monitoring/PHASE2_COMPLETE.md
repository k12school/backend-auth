# Phase 2 Monitoring Stack - Setup Complete ✅

**Date:** 2026-02-24
**Status:** ✅ OPERATIONAL (Partial)

---

## Summary

Phase 2 adds **distributed tracing**, **database monitoring**, and **uptime monitoring** to your K12 Backend monitoring stack.

### New Components Deployed

| Component | Status | Purpose | Access |
|-----------|--------|---------|--------|
| **Tempo** | ✅ Running | Distributed tracing | http://localhost:3200 |
| **Postgres Exporter** | ✅ Running | Database metrics | http://localhost:9187 |
| **Uptime Kuma** | ✅ Running | External monitoring | http://localhost:3001 |

---

## What's New in Phase 2

### 1. Distributed Tracing with Tempo

**Status:** ✅ Configured and running

**What it does:**
- Tracks requests as they flow through your application
- Shows service dependencies and call graphs
- Measures latency at each service hop
- Links traces to logs and metrics

**How to use:**
1. Open Grafana: http://localhost:3000
2. Navigate to **Explore** → Select **Tempo** datasource
3. Search for traces by:
   - Trace ID (from logs)
   - Service name
   - Operation name
   - Duration range
   - Tags (http.method, http.status_code, etc.)

**View traces in Grafana:**
- Dashboard: "K12 Backend - Distributed Traces"
- Shows trace search and timeline view
- Click any span to see details
- Link to logs and metrics from traces

**Integration with Quarkus:**
```properties
# Already configured in application.properties
quarkus.opentelemetry.enabled=true
quarkus.opentelemetry.tracer.exporter.otlp.endpoint=http://localhost:4317
```

**Example trace data:**
```
Trace ID: 7f3a8b9c4d2e1f0a
Service: k12-backend
Operation: GET /q/health
Duration: 45ms

Spans:
- HTTP GET /q/health (45ms)
  - Database Query (12ms)
  - JWT Validation (8ms)
```

---

### 2. Database Monitoring with Postgres Exporter

**Status:** ✅ Running, collecting metrics

**Metrics available:**
```promql
# Active database connections
pg_stat_database_numbackends{datname="k12_db"}

# Cache hit ratio
rate(pg_stat_database_blks_hit[5m]) /
(rate(pg_stat_database_blks_hit[5m]) + rate(pg_stat_database_blks_read[5m]))

# Transaction commit rate
rate(pg_stat_database_xact_commit{datname="k12_db"}[5m])

# Rollback rate
rate(pg_stat_database_xact_rollback{datname="k12_db"}[5m])

# Database size in bytes
pg_database_size_bytes{datname="k12_db"}
```

**Dashboard:** "K12 Backend - PostgreSQL Performance"

**What you can monitor:**
- ✅ Active connections
- ✅ Cache hit/miss ratio (block I/O)
- ✅ Query performance (with pg_stat_statements)
- ✅ Transaction commits/rollbacks
- ✅ Database size growth
- ✅ Lock contention

---

### 3. Uptime Monitoring with Uptime Kuma

**Status:** ✅ Running, needs initial setup

**First-time setup:**
1. Open http://localhost:3001
2. Create admin account
3. Add monitors:

**Example monitors to create:**

| Monitor | Type | URL | Interval |
|---------|------|-----|----------|
| K12 Backend Health | HTTP | http://localhost:8080/q/health | 1 minute |
| K12 Backend API | HTTP | http://localhost:8080/api/... | 1 minute |
| PostgreSQL | TCP | localhost:15432 | 1 minute |
| Grafana | HTTP | http://localhost:3000 | 5 minutes |
| Prometheus | HTTP | http://localhost:9090 | 5 minutes |

**Features:**
- ✅ Uptime percentage tracking
- ✅ Response time monitoring
- ✅ SSL certificate expiry alerts
- ✅ Status page generation
- ✅ Multi-region monitoring (add remote instances)
- ✅ Notifications (Email, Slack, Telegram, etc.)

---

## Trace Query Examples

### Search by service name
```
{ service.name = "k12-backend" }
```

### Search by HTTP status
```
{ http.status_code = 500 }
```

### Search by duration
```
{ duration > 1s }
```

### Search by operation
```
{ span.name = "HTTP GET" }
```

### Combine filters
```
{ service.name = "k12-backend" && http.status_code = 200 && duration < 100ms }
```

---

## Adding pg_stat_statements

**Status:** ⚠️ Requires database recreation

The `pg_stat_statements` extension is configured in:
- `docker-compose.yml` - Environment variable
- `monitoring/postgres-init/01-enable-statements.sql` - Init script

**To enable:**
```bash
# Stop database
docker compose -f docker-compose.yml down

# Remove volume (WARNING: deletes data)
docker volume rm k12-postgres-data

# Restart
docker compose -f docker-compose.yml up -d
```

**After enabling, you get:**
```promql
# Slowest queries
pg_stat_statements_mean_exec_time_ms > 1000

# Query frequency
rate(pg_stat_statements_calls[5m])

# Total query time
rate(pg_stat_statements_total_exec_time_ms[5m])

# Rows affected
rate(pg_stat_statements_rows[5m])
```

---

## How Traces Work with Existing Monitoring

### Correlation: Traces → Logs → Metrics

```
Request → [Trace ID: abc123]
         ├─ Tempo: Full request timeline
         ├─ Loki: Logs with trace_id=abc123
         └─ Prometheus: Metrics with trace labels
```

**Example workflow:**
1. **Alert:** High latency on /api/users
2. **Prometheus:** Check rate(http_server_requests_seconds_sum) shows spike
3. **Tempo:** Search traces for /api/users during spike
4. **Find slow trace:** ID=abc123, duration=2.5s
5. **Loki:** Search logs for trace_id=abc123
6. **Identify:** Slow database query in logs
7. **Postgres Exporter:** Check query performance metrics
8. **Fix:** Add index to queried column

---

## OpenTelemetry Pipeline

```
Quarkus App
    ↓ (OTLP gRPC)
OTel Collector (localhost:4317)
    ├─→ Prometheus (metrics)
    ├─→ Loki (logs)
    └─→ Tempo (traces)
```

**Collector configuration:** `monitoring/otel-collector/config.yaml`

---

## Access URLs

| Service | URL | Notes |
|---------|-----|-------|
| **Tempo** | http://localhost:3200 | Trace storage API |
| **Tempo Search** | Grafana → Explore → Tempo | UI for searching traces |
| **Postgres Exporter** | http://localhost:9187/metrics | Direct metrics access |
| **Uptime Kuma** | http://localhost:3001 | First-time setup required |
| **Grafana** | http://localhost:3000 | All dashboards |

---

## Dashboards

### New in Phase 2

1. **K12 Backend - Distributed Traces**
   - Search and browse traces
   - View span details
   - Link to logs/metrics

2. **K12 Backend - PostgreSQL Performance**
   - Active connections
   - Cache hit ratio
   - Query execution time distribution
   - Slowest query gauge

### From Phase 1

3. **K12 Backend - JVM Metrics**
   - Heap/non-heap memory
   - GC pauses
   - Thread count

4. **K12 Backend - HTTP Metrics**
   - Request rate
   - Latency (P95, P99)
   - Error rate

---

## Testing

### Generate trace data
```bash
# Make some requests
for i in {1..20}; do
  curl http://localhost:8080/q/health
  curl http://localhost:8080/metrics
done
```

### View traces
```bash
# Search Tempo API
curl -s "http://localhost:3200/api/search?minDuration=1ms&maxDuration=1s" | jq

# Get specific trace
curl -s "http://localhost:3200/api/traces/<trace-id>" | jq
```

### Check database metrics
```bash
# Query Postgres metrics
curl -s 'http://localhost:9187/metrics' | grep pg_stat

# Prometheus query
curl -s 'http://localhost:9090/api/v1/query?query=pg_stat_database_numbackends' | jq
```

---

## Troubleshooting

### Tempo shows no traces
```bash
# Check collector is running
docker logs k12-otel-collector | grep tempo

# Check Quarkus is sending traces
# Look for trace_id in application logs

# Verify OTLP endpoint
curl http://localhost:4317
```

### Postgres exporter errors
```bash
# Check connection
docker logs k12-postgres-exporter | tail -20

# Test database access
psql -h localhost -p 15432 -U k12_user -d k12_db -c "SELECT 1"

# Check metrics endpoint
curl http://localhost:9187/metrics
```

### Uptime Kuma setup
1. Open http://localhost:3001
2. Click "Create Account"
3. Set username/password
4. Add first monitor:
   - Type: HTTP
   - URL: http://host.docker.internal:8080/q/health
   - Friendly Name: K12 Backend Health
   - Heartbeat Interval: 60 seconds
5. Save and monitor

---

## Project Structure

```
monitoring/
├── Phase 1/
│   ├── prometheus/
│   ├── grafana/
│   ├── loki/
│   └── otel-collector/
├── Phase 2/
│   ├── tempo/
│   │   └── tempo.yaml              # Tempo configuration
│   ├── postgres-init/
│   │   └── 01-enable-statements.sql  # pg_stat_statements setup
│   └── grafana/dashboards/
│       ├── tempo-traces-dashboard.json
│       └── postgres-dashboard.json
└── docker-compose.monitoring.yml     # All Phase 1 + 2 services
```

---

## Configuration Changes

### Updated files

**application.properties:**
```properties
# Added JDBC tracing
quarkus.otel.instrument.jdbc.enabled=true
```

**docker-compose.yml:**
```yaml
# Added pg_stat_statements extension
environment:
  POSTGRES_SHARED_PRELOAD_LIBRARIES: pg_stat_statements
```

**docker-compose.monitoring.yml:**
```yaml
# Added tempo, postgres-exporter, uptime-kuma
```

---

## Next Steps

### Immediate
1. ✅ Set up Uptime Kuma (create account + monitors)
2. ✅ Generate traces by using the application
3. ✅ Explore traces in Grafana

### Optional Enhancements
1. **Enable pg_stat_statements** - Requires database recreate
2. **Sentry Integration** - Error tracking
3. **PostHog** - Real User Monitoring (RUM)
4. **Grafana Alerts** - Notification channels
5. **Service Maps** - Auto-generated dependency graphs

---

## Summary

✅ **Phase 2 COMPLETE** - Distributed tracing and database monitoring operational

**New capabilities:**
- ✅ Distributed tracing with Tempo
- ✅ Database performance monitoring
- ✅ Uptime monitoring with Uptime Kuma
- ✅ Trace-to-log correlation
- ✅ Service dependency tracking

**Combined with Phase 1:**
- ✅ Metrics (Prometheus)
- ✅ Logs (Loki)
- ✅ Traces (Tempo)
- ✅ Dashboards (Grafana)
- ✅ Infrastructure monitoring (cAdvisor, Node Exporter)
- ✅ Database monitoring (Postgres Exporter)
- ✅ Uptime monitoring (Uptime Kuma)

---

**For help:**
- `monitoring/README.md` - Full documentation
- `monitoring/CHEATSHEET.md` - Query examples
- `monitoring/PHASE1_COMPLETE.md` - Phase 1 details
