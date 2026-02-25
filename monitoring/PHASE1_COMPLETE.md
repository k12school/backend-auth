# Phase 1 Monitoring Stack - Setup Complete ✅

## Status: **FULLY OPERATIONAL** ✅

**Test Date:** 2026-02-24
**All Components:** Running and verified

---

## Summary

Your K12 Backend now has a complete **Phase 1 monitoring stack** running and tested:

| Component | Status | Purpose | Access |
|-----------|--------|---------|--------|
| **Prometheus** | ✅ Running | Metrics collection & storage | http://localhost:9090 |
| **Grafana** | ✅ Running | Dashboards & visualization | http://localhost:3000 |
| **Loki** | ✅ Running | Log aggregation | Internal (3100) |
| **Promtail** | ✅ Running | Log collector | Internal (9080) |
| **OTel Collector** | ✅ Running | Telemetry pipeline | http://localhost:4317 |
| **cAdvisor** | ✅ Running | Container metrics | http://localhost:8081 |
| **Node Exporter** | ✅ Running | Host metrics | http://localhost:9100 |
| **Quarkus App** | ✅ Monitored | Application metrics | http://localhost:8080/metrics |

## What's Being Monitored

### ✅ JVM Metrics
- Heap memory usage (by memory pool)
- Non-heap memory (Metaspace, Code Cache)
- Thread counts (by state)
- GC pause times
- CPU usage

### ✅ HTTP Metrics
- Request count (by method, URI, status)
- Request latency (sum, max)
- Request rate

### ✅ Database Metrics
- Connection pool stats (via Quarkus Agroal)
- Query duration (when custom metrics added)

### ✅ Infrastructure Metrics
- Container resource usage (CPU, memory, disk)
- Host metrics (CPU, memory, disk, network)

### ✅ Logs
- Application logs collected from `log/k12-backend.log`
- Parsed with structured fields (level, logger, thread)
- Indexed by labels for fast search

## Quick Start Guide

### 1. View Your Dashboards

**Grafana:** http://localhost:3000
- Login: `admin` / `admin`
- Pre-built dashboards:
  - **K12 Backend - JVM Metrics** - Memory, GC, threads
  - **K12 Backend - HTTP Metrics** - Requests, latency, errors

**Prometheus:** http://localhost:9090
- Browse metrics: http://localhost:9090/graph
- Check targets: http://localhost:9090/targets
- Query examples:
  ```promql
  # Heap usage percentage
  (jvm_memory_used_bytes{area="heap",id="G1 Old Gen"} / jvm_memory_max_bytes{area="heap",id="G1 Old Gen"}) * 100

  # Request rate
  rate(http_server_requests_seconds_count[5m])

  # Average latency
  rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
  ```

### 2. Search Logs

In Grafana:
1. Go to **Explore** (left sidebar)
2. Select **Loki** datasource
3. Use LogQL queries:
  ```logql
  # All logs
  {job="k12-backend"}

  # Errors only
  {job="k12-backend"} |= "ERROR"

  # By logger
  {job="k12-backend"} |= "com.k12"
  ```

### 3. Add Custom Metrics

See the example at `monitoring/example/MetricsService.java`:

```java
@Inject
MeterRegistry registry;

// Counter
Counter.builder("user.logins")
    .tags("tenant", tenantId)
    .register(registry)
    .increment();

// Timer
Timer.builder("database.query.duration")
    .tags("query_type", "SELECT")
    .register(registry)
    .record(durationMs, TimeUnit.MILLISECONDS);

// Gauge
Gauge.builder("cache.size", cache, Cache::size)
    .tags("cache", "userCache")
    .register(registry);
```

### 4. Restart Services

```bash
# Start monitoring stack
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# Stop monitoring stack
docker compose -f docker-compose.monitoring.yml down

# View logs
docker compose -f docker-compose.monitoring.yml logs -f prometheus
docker compose -f docker-compose.monitoring.yml logs -f grafana
docker compose -f docker-compose.monitoring.yml logs -f loki
```

## File Structure

```
k12/back/
├── docker-compose.yml                    # Database
├── docker-compose.monitoring.yml         # Monitoring stack
├── monitoring/
│   ├── README.md                         # Complete documentation
│   ├── CHEATSHEET.md                     # Quick reference
│   ├── .env.example                      # Environment variables
│   ├── example/
│   │   ├── MetricsService.java           # Custom metrics example
│   │   └── MetricsServiceTest.java       # Test examples
│   ├── prometheus/
│   │   ├── prometheus.yml                # Scrape configs
│   │   └── alerts/
│   │       └── application.yml           # Alert rules
│   ├── loki/
│   │   └── config.yml                    # Loki config
│   ├── promtail/
│   │   └── config.yml                    # Log collector
│   ├── otel-collector/
│   │   └── config.yaml                   # OTel pipeline
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/              # Auto-configured datasources
│       │   └── dashboards/               # Dashboard provisioning
│       └── dashboards/
│           ├── quarkus-jvm-dashboard.json
│           └── http-metrics-dashboard.json
└── src/main/resources/
    └── application.properties            # Updated with OTel config
```

## Alert Rules (Pre-configured)

Prometheus is configured with these alerts (in `monitoring/prometheus/alerts/application.yml`):

- **HighErrorRate** - Error rate > 5% for 5 minutes
- **HighLatency** - P95 latency > 1s for 5 minutes
- **ServiceDown** - Application not responding for 1 minute
- **HighMemoryUsage** - Heap usage > 90% for 5 minutes
- **LongGcPause** - Excessive GC time
- **HighCpuUsage** - CPU usage > 80% for 5 minutes
- **DiskSpaceLow** - Disk space < 10%

To add alert notifications (email, Slack, etc.), configure an Alertmanager in Phase 2.

## Next Steps - Phase 2

When ready to extend monitoring:

1. **Tempo** - Distributed tracing (request flows across services)
2. **Sentry** - Error tracking & stack traces
3. **pg_stat_statements** - PostgreSQL query performance
4. **Uptime Kuma** - External uptime monitoring
5. **PostHog** - Real User Monitoring (RUM)

## Troubleshooting

**Can't access Grafana?**
```bash
# Restart Grafana
docker compose -f docker-compose.monitoring.yml restart grafana
```

**No metrics in Prometheus?**
```bash
# Check targets: http://localhost:9090/targets
# Verify Quarkus is running
curl http://localhost:8080/metrics
```

**No logs in Loki?**
```bash
# Check log file exists
ls -la log/k12-backend.log

# Restart Promtail
docker compose -f docker-compose.monitoring.yml restart promtail
```

## Resources

- [monitoring/README.md](monitoring/README.md) - Complete documentation
- [monitoring/CHEATSHEET.md](monitoring/CHEATSHEET.md) - Query examples & tips
- [monitoring/example/MetricsService.java](monitoring/example/MetricsService.java) - Custom metrics code

---

**Phase 1 Status:** ✅ Complete

**What you can now do:**
- ✅ Monitor application performance in real-time
- ✅ View JVM health (memory, GC, threads)
- ✅ Track HTTP request metrics
- ✅ Search and analyze logs
- ✅ Visualize metrics in Grafana dashboards
- ✅ Query metrics with PromQL
- ✅ Set up alert rules
