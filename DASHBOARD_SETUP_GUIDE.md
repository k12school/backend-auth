# SigNoz Dashboard Setup Guide

**Created**: 3 empty dashboards
**Access**: http://localhost:3301/dashboards

## 1. k12-backend - Application Overview

**ID**: `k12-app-overview-001`

### Panel 1: HTTP Request Rate
- **Title**: HTTP Request Rate
- **Description**: Requests per second
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  sum(value) as rate
FROM signoz_metrics.samples_v4
WHERE metric_name = 'http.server.requests.count'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 2: HTTP Request Duration
- **Title**: Avg Request Duration
- **Description**: Average HTTP request latency
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  avg(value) as duration_ms
FROM signoz_metrics.samples_v4
WHERE metric_name = 'http.server.requests.avg'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 3: JVM Heap Memory
- **Title**: Heap Memory Usage
- **Description**: Heap memory by area
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  toString(JSONExtractString(labels, 'area')) as area,
  avg(value) as bytes
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.memory.used'
  AND JSONExtractString(labels, 'area') LIKE '%heap%'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time, area
ORDER BY time DESC
```

### Panel 4: GC Pauses
- **Title**: GC Pause Time
- **Description**: Garbage collection pause duration
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  sum(value) as gc_time_ms
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.gc.pause.sum'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 5: Active Connections
- **Title**: Active HTTP Connections
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  avg(value) as connections
FROM signoz_metrics.samples_v4
WHERE metric_name = 'http.server.active.connections'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

---

## 2. k12-backend - Database Pool

**ID**: `k12-db-pool-002`

### Panel 1: Active Connections
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  avg(value) as active
FROM signoz_metrics.samples_v4
WHERE metric_name = 'agroal.active.count'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 2: Available Connections
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  avg(value) as available
FROM signoz_metrics.samples_v4
WHERE metric_name = 'agroal.available.count'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 3: Awaiting Connections
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  avg(value) as awaiting
FROM signoz_metrics.samples_v4
WHERE metric_name = 'agroal.awaiting.count'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 4: Max Used Connections
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  max(value) as max_used
FROM signoz_metrics.samples_v4
WHERE metric_name = 'agroal.max.used.count'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 5: Acquisition Rate
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  sum(value) as acquire_rate
FROM signoz_metrics.samples_v4
WHERE metric_name = 'agroal.acquire.count'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

---

## 3. k12-backend - JVM Runtime

**ID**: `k12-jvm-runtime-003`

### Panel 1: Heap Memory
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  toString(JSONExtractString(labels, 'area')) as area,
  avg(value) as bytes
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.memory.used'
  AND JSONExtractString(labels, 'area') LIKE '%heap%'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time, area
ORDER BY time DESC
```

### Panel 2: Non-Heap Memory
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  toString(JSONExtractString(labels, 'area')) as area,
  avg(value) as bytes
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.memory.used'
  AND JSONExtractString(labels, 'area') NOT LIKE '%heap%'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time, area
ORDER BY time DESC
```

### Panel 3: Live Threads
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  avg(value) as threads
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.threads.live'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 4: Peak Threads
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  max(value) as peak_threads
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.threads.peak'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 5: Daemon Threads
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  avg(value) as daemon_threads
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.threads.daemon'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 6: GC Count
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  sum(value) as gc_count
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.gc.pause.count'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 7: GC Time
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  sum(value) as gc_time_ms
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.gc.pause.sum'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 8: CPU Utilization
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  avg(value) * 100 as cpu_percent
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.cpu.recent_utilization'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Panel 9: Loaded Classes
- **Query**:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  avg(value) as classes
FROM signoz_metrics.samples_v4
WHERE metric_name = 'jvm.classes.loaded'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

---

## How to Add Panels to Dashboards

### Method 1: Via UI

1. Open SigNoz: http://localhost:3301
2. Go to **Dashboards** tab
3. Select one of the new dashboards:
   - "k12-backend - Application Overview"
   - "k12-backend - Database Pool"
   - "k12-backend - JVM Runtime"
4. Click **"+ Add Panel"**
5. Click **"QueryBuilder"** → **"Metrics"** → **"Raw Query"**
6. Paste the query from above
7. Set the title and description
8. Click **"Save"**

### Method 2: Clone & Modify Existing Panels

1. In the dashboard, click **"+ Add Panel"**
2. Choose **"Import from Library"**
3. Search for similar panels (e.g., "JVM Memory", "HTTP")
4. Import and modify the query to match our metric names

---

## Quick Verification

After adding panels, you should see:
- ✅ Real-time graphs updating every 5 seconds
- ✅ Multiple time series (different areas, types)
- ✅ Proper legends and labels

### Troubleshooting

**If panel shows "No data":**
- Check query syntax in ClickHouse client
- Verify metric name exists: `SELECT metric_name FROM signoz_metrics.time_series_v4`
- Check time window: Increase `INTERVAL 15 MINUTE` to `INTERVAL 1 HOUR`

**If query has syntax error:**
- Ensure backticks are used around labels like `'area'` not `area`
- Check that `json_extract` parameters are correct
- Verify table names: `signoz_metrics.samples_v4` (not `time_series_v4`)

---

## Available Metrics Reference

All available metrics can be listed with:

```bash
docker exec k12-clickhouse clickhouse-client --query \
  "SELECT DISTINCT metric_name FROM signoz_metrics.time_series_v4 \
   WHERE metric_name LIKE 'jvm.%' OR metric_name LIKE 'http.%' \
   OR metric_name LIKE 'agroal.%' ORDER BY metric_name"
```

**Key Metrics**:
- HTTP: `http.server.requests.*`, `http.server.active.connections`
- JVM: `jvm.memory.*`, `jvm.gc.*`, `jvm.threads.*`, `jvm.cpu.*`
- DB Pool: `agroal.active.count`, `agroal.available.count`, `agroal.*`
- PostgreSQL: `pg_stat_activity.*`, `pg_stat_archiver.*`, `pg_locks.*`
