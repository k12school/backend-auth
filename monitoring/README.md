# K12 Backend - Phase 1 Monitoring Stack

Complete open-source monitoring solution for K12 Backend using Prometheus, Grafana, Loki, and OpenTelemetry.

## What's Included

### Phase 1 Components

| Component | Purpose | Port | Access |
|-----------|---------|------|--------|
| **Prometheus** | Metrics collection & storage | 9090 | http://localhost:9090 |
| **Grafana** | Dashboards & visualization | 3000 | http://localhost:3000 (admin/admin) |
| **Loki** | Log aggregation | 3100 | Internal |
| **Promtail** | Log collector | 9080 | http://localhost:9080 |
| **OTel Collector** | Telemetry data pipeline | 4317/4318 | http://localhost:4317 |
| **cAdvisor** | Container metrics | 8081 | http://localhost:8081 |
| **Node Exporter** | Host metrics | 9100 | http://localhost:9100 |

## Quick Start

### 1. Start the monitoring stack

```bash
# Start both database and monitoring
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# Or start only monitoring (if database already running)
docker-compose -f docker-compose.monitoring.yml up -d
```

### 2. Start your Quarkus application

```bash
./gradlew quarkusDev
```

The application will automatically:
- Expose metrics at `http://localhost:8080/metrics` (Prometheus)
- Send traces to OpenTelemetry Collector
- Write logs to `log/k12-backend.log` (collected by Promtail)

### 3. Access dashboards

**Grafana:** http://localhost:3000
- Default credentials: `admin` / `admin`
- Pre-configured dashboards:
  - **K12 Backend - JVM Metrics** (memory, GC, threads)
  - **K12 Backend - HTTP Metrics** (requests, latency, errors)

**Prometheus:** http://localhost:9090
- Query metrics directly
- Check targets status: http://localhost:9090/targets

## Available Metrics

### HTTP Metrics
- `http_server_requests_seconds_count` - Request count by endpoint
- `http_server_requests_seconds_sum` - Request latency sum
- `http_server_requests_seconds_bucket` - Request latency histogram

Query examples:
```promql
# Request rate by endpoint
rate(http_server_requests_seconds_count[5m])

# P95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### JVM Metrics
- `jvm_memory_used_bytes` - Memory usage
- `jvm_memory_max_bytes` - Max memory
- `jvm_gc_pause_seconds` - GC pause time
- `jvm_thread_count` - Thread count

Query examples:
```promql
# Heap usage percentage
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# GC rate
rate(jvm_gc_pause_seconds_count[5m])
```

### Database Metrics
- `jdbc_connections_active` - Active DB connections
- `jdbc_connections_max` - Max DB connections
- `jdbc_connections_min` - Min DB connections

Query examples:
```promql
# Connection pool usage
jdbc_connections_active / jdbc_connections_max * 100
```

## Log Exploration

Logs from `log/k12-backend.log` are automatically collected and indexed in Loki.

**Search in Grafana:**
1. Go to http://localhost:3000
2. Click "Explore" → Select "Loki" datasource
3. Use LogQL queries:

```logql
# All logs
{job="k12-backend"}

# Errors only
{job="k12-backend"} |= "ERROR"

# Specific tenant
{job="k12-backend"} |= "tenant_id"

# Trace correlation (with trace_id)
{job="k12-backend"} |= "trace_id=abc123"
```

## Project Structure

```
monitoring/
├── prometheus/
│   ├── prometheus.yml          # Main config
│   └── alerts/
│       └── application.yml     # Alert rules
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/        # Auto-provision datasources
│   │   └── dashboards/         # Dashboard provisioning
│   └── dashboards/
│       ├── quarkus-jvm-dashboard.json
│       └── http-metrics-dashboard.json
├── loki/
│   └── config.yml              # Loki config
├── promtail/
│   └── config.yml              # Log collector config
└── otel-collector/
    └── config.yaml             # OTel collector config
```

## Custom Metrics in Code

### Add custom metrics to your Quarkus application:

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MetricsService {

    @Inject
    MeterRegistry registry;

    // Counter metric
    public void incrementUserLogin(String tenantId) {
        Counter.builder("user.logins")
            .tag("tenant", tenantId)
            .register(registry)
            .increment();
    }

    // Timer metric
    public void recordDbQueryTime(String queryType, long durationMs) {
        Timer.builder("database.query.duration")
            .tag("query_type", queryType)
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // Gauge metric
    public void registerActiveConnectionsGauge() {
        Gauge.builder("connections.active", connectionPool, pool -> pool.getActiveConnections())
            .tag("pool", "tenant-db")
            .register(registry);
    }
}
```

## Troubleshooting

### Check if services are running

```bash
docker-compose -f docker-compose.monitoring.yml ps
```

### Check service logs

```bash
# All monitoring services
docker-compose -f docker-compose.monitoring.yml logs -f

# Specific service
docker-compose -f docker-compose.monitoring.yml logs -f prometheus
```

### Verify Prometheus is scraping

1. Go to http://localhost:9090/targets
2. Check that `k12-backend` target is **UP**
3. Click the target to see scrape errors

### Verify logs are flowing

1. Go to Grafana → Explore
2. Select Loki datasource
3. Run: `{job="k12-backend"}`
4. Should see recent logs

### Common Issues

**Issue: Prometheus can't scrape Quarkus**
- Ensure Quarkus is running: `curl http://localhost:8080/metrics`
- Check firewall: `curl http://localhost:8080` from within Docker

**Issue: No logs in Loki**
- Check log file exists: `ls -la log/k12-backend.log`
- Restart Promtail: `docker-compose -f docker-compose.monitoring.yml restart promtail`
- Check Promtail logs: `docker-compose -f docker-compose.monitoring.yml logs promtail`

**Issue: Grafana can't connect to datasources**
- Check datasources are configured: http://localhost:3000/datasources
- Restart Grafana: `docker-compose -f docker-compose.monitoring.yml restart grafana`

## Next Steps (Phase 2)

- **Tempo**: Distributed tracing
- **Sentry**: Error tracking
- **pg_stat_statements**: Database query monitoring
- **Uptime Kuma**: External monitoring

## Maintenance

### Stop monitoring stack

```bash
docker-compose -f docker-compose.monitoring.yml down
```

### Clean up data volumes

```bash
docker-compose -f docker-compose.monitoring.yml down -v
```

### Update configurations

After editing config files:
```bash
docker-compose -f docker-compose.monitoring.yml up -d --force-recreate
```

## Security Notes

**For Development Only:**
- Default Grafana password: `admin/admin`
- No authentication on Prometheus
- No TLS on connections

**Before Production:**
1. Change all default passwords
2. Enable TLS/SSL
3. Configure authentication
4. Restrict network access
5. Use secrets management
6. Enable audit logging

## Resources

- [Prometheus Querying](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboards](https://grafana.com/docs/grafana/latest/dashboards/)
- [Loki LogQL](https://grafana.com/docs/loki/latest/logql/)
- [Quarkus Micrometer](https://quarkus.io/guides/micrometer)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
