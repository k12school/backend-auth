# Monitoring Stack Cheatsheet

## Quick Commands

```bash
# Start monitoring stack
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# View logs
docker-compose -f docker-compose.monitoring.yml logs -f

# Stop everything
docker-compose -f docker-compose.monitoring.yml down

# Restart specific service
docker-compose -f docker-compose.monitoring.yml restart prometheus

# Check service status
docker-compose -f docker-compose.monitoring.yml ps
```

## Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Prometheus Targets | http://localhost:9090/targets | - |
| cAdvisor | http://localhost:8081 | - |
| Quarkus Metrics | http://localhost:8080/metrics | - |

## Common Prometheus Queries

### HTTP Performance

```promql
# Request rate (requests per second)
rate(http_server_requests_seconds_count[5m])

# P95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# P99 latency
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))

# Average latency
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# Error rate (5xx errors)
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Requests by status code
sum(rate(http_server_requests_seconds_count[5m])) by (status)

# Top 10 slowest endpoints
topk(10, rate(http_server_requests_seconds_sum[5m]) by (uri))
```

### JVM Metrics

```promql
# Heap usage %
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# Non-heap usage %
(jvm_memory_used_bytes{area="nonheap"} / jvm_memory_max_bytes{area="nonheap"}) * 100

# GC pause rate
rate(jvm_gc_pause_seconds_sum[5m])

# GC count rate
rate(jvm_gc_pause_seconds_count[5m])

# Thread count
jvm_thread_count_live

# CPU usage
rate(process_cpu_seconds_total[5m]) * 100
```

### Database

```promql
# Active connections
jdbc_connections_active

# Connection pool usage %
jdbc_connections_active / jdbc_connections_max * 100

# Idle connections
jdbc_connections_idle
```

### System/Container

```promql
# Container CPU usage
rate(container_cpu_usage_seconds_total{name=~".*k12.*"}[5m]) * 100

# Container memory usage
container_memory_usage_bytes{name=~".*k12.*"}

# Host memory usage
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100

# Disk usage
(node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes * 100
```

## LogQL Queries (Loki)

```logql
# All application logs
{job="k12-backend"}

# Error logs only
{job="k12-backend"} |= "ERROR"

# Logs with specific tenant
{job="k12-backend"} |= "tenant_id=123"

# Logs in last 15 minutes
{job="k12-backend"} | line_format "{{.timestamp}} {{.message}}" | timestamp > now - 15m

# Count errors in last hour
count_over_time({job="k12-backend"} |= "ERROR" [1h])

# Rate of errors per minute
rate({job="k12-backend"} |= "ERROR" [1m])

# Filter by multiple conditions
{job="k12-backend"} |= "ERROR" != "timeout"

# Extract field with regex
{job="k12-backend"} | regexp `(?P<tenant>tenant_id=\w+)` | line_format "{{.tenant}}"
```

## Grafana Tips

### Keyboard Shortcuts
- `t` - Open time picker
- `d` - Open dashboard search
- `k` - Toggle panel edit mode
- `Ctrl+S` - Save dashboard

### Panel Types
- **Time Series** - Trends over time
- **Stat** - Single value with sparkline
- **Gauge** - Percentage/value gauge
- **Table** - Tabular data
- **Bar Gauge** - Horizontal/vertical bars
- **Pie Chart** - Distribution

### Variables
Create variables for dynamic dashboards:
- `${interval}` - Time interval for queries
- `${instance}` - Filter by instance
- `${job}` - Filter by job

## Alert Rules

Prometheus alerts are in `monitoring/prometheus/alerts/application.yml`:

```yaml
# High error rate (> 5% for 5 minutes)
HighErrorRate

# High latency (P95 > 1s for 5 minutes)
HighLatency

# Service down
ServiceDown

# High heap usage (> 90% for 5 minutes)
HighMemoryUsage

# Long GC pauses
LongGcPause

# Low disk space (< 10%)
DiskSpaceLow
```

## Troubleshooting Commands

```bash
# Test Prometheus endpoint
curl http://localhost:8080/metrics

# Test from inside Docker network
docker exec k12-prometheus wget -qO- http://host.docker.internal:8080/metrics

# Check Promtail is reading logs
docker exec k12-promtail wget -qO- http://localhost:9080/metrics

# View OTel collector metrics
curl http://localhost:8889/metrics

# Reload Prometheus config (without restart)
curl -X POST http://localhost:9090/-/reload

# Check Grafana datasource health
curl http://admin:admin@localhost:3000/api/datasources
```

## Custom Metrics Quick Reference

```java
// Counter
Counter.builder("metric.name")
    .tag("label", "value")
    .register(registry)
    .increment();

// Timer
Timer.builder("operation.duration")
    .tag("operation", "type")
    .register(registry)
    .record(() -> {
        // code to time
    });

// Gauge
Gauge.builder("cache.size", cache, Cache::size)
    .register(registry);
```

## Dashboard Templates

### JVM Overview
- Heap Memory Used/Committed/Max
- Heap Usage % (gauge)
- GC Pause Time Rate
- GC Count Rate

### HTTP Performance
- Request Rate by Endpoint
- Average Latency by Endpoint
- P95/P99 Latency
- Error Rate by Status

### Database
- Active Connections
- Connection Pool Usage %
- Query Duration (custom metric)

### System
- Host CPU Usage
- Host Memory Usage
- Container CPU/Memory
- Disk Space
