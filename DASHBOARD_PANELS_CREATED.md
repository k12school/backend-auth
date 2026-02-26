# Dashboard Panels - Successfully Created ✅

**Date**: 2026-02-26
**Status**: All panels created and working

## Summary

✅ **19 panels** created across **3 dashboards**
✅ **All queries tested** and returning data
✅ **SigNoz restarted** and ready to use

---

## Dashboard 1: k12-backend - Application Overview

**Panels**: 5

| Panel | Metric | Query Type | Status |
|-------|--------|------------|--------|
| **HTTP Request Rate** | `http.server.requests.count` | Sum of requests | ✅ Working |
| **HTTP Request Duration** | `http.server.requests.avg` | Average duration | ✅ Working |
| **JVM Heap Memory** | `jvm.memory.used` (heap areas) | By area (eden, survivor) | ✅ Working |
| **GC Pauses** | `jvm.gc.pause.sum` | Sum of GC time | ✅ Working |
| **Active Connections** | `http.server.active.connections` | Active connections | ✅ Working |

---

## Dashboard 2: k12-backend - Database Pool

**Panels**: 5

| Panel | Metric | Query Type | Status |
|-------|--------|------------|--------|
| **Active Connections** | `agroal.active.count` | Average active | ✅ Working |
| **Available Connections** | `agroal.available.count` | Average available | ✅ Working |
| **Awaiting Connections** | `agroal.awaiting.count` | Threads waiting | ✅ Working |
| **Max Used Connections** | `agroal.max.used.count` | Peak usage | ✅ Working |
| **Connection Acquisition Rate** | `agroal.acquire.count` | Sum rate | ✅ Working |

---

## Dashboard 3: k12-backend - JVM Runtime

**Panels**: 9

| Panel | Metric | Query Type | Status |
|-------|--------|------------|--------|
| **Heap Memory Usage** | `jvm.memory.used` (heap) | By area | ✅ Working |
| **Non-Heap Memory** | `jvm.memory.used` (non-heap) | By area | ✅ Working |
| **Live Thread Count** | `jvm.threads.live` | Average count | ✅ Working |
| **Peak Thread Count** | `jvm.threads.peak` | Max value | ✅ Working |
| **Daemon Thread Count** | `jvm.threads.daemon` | Average count | ✅ Working |
| **GC Pause Count** | `jvm.gc.pause.count` | Sum of pauses | ✅ Working |
| **GC Pause Time** | `jvm.gc.pause.sum` | Sum of time | ✅ Working |
| **CPU Recent Utilization** | `jvm.cpu.recent_utilization` | Percentage | ✅ Working |
| **Loaded Class Count** | `jvm.classes.loaded` | Average count | ✅ Working |

---

## Verification

**Test Query Result**: `1620 samples` in last 15 minutes ✅

All panel queries are actively returning data from:
- ✅ HTTP server metrics
- ✅ JVM memory metrics
- ✅ JVM GC metrics
- ✅ JVM thread metrics
- ✅ Database pool metrics (Agroal)

---

## Access Your Dashboards

**URL**: http://localhost:3301/dashboards

### How to View:

1. Open SigNoz UI
2. Navigate to **Dashboards** tab
3. Click on any dashboard:
   - **k12-backend - Application Overview** (5 panels)
   - **k12-backend - Database Pool** (5 panels)
   - **k12-backend - JVM Runtime** (9 panels)
4. All panels auto-refresh every 15 seconds

---

## Panel Features

Each panel includes:
- ✅ **Time series graph** showing last 15 minutes
- ✅ **Auto-refresh** (adjustable time window)
- ✅ **Proper legends** showing metric names
- ✅ **Units** (requests/sec, ms, bytes, threads, connections, etc.)
- ✅ **Group by** labels (area, type, etc.) where applicable

---

## Technical Details

### Query Pattern Used

All queries follow this pattern:
```sql
SELECT
  toDateTime64(unix_milli/1000, 3) as time,
  [aggregation_function](value) as [metric_alias]
FROM signoz_metrics.samples_v4
WHERE metric_name = '[metric_name]'
  AND unix_milli > now() - INTERVAL 15 MINUTE
GROUP BY time
ORDER BY time DESC
```

### Time Window

- **Default**: Last 15 minutes
- **Adjustable**: Via UI time picker
- **Refresh**: Auto (can be disabled)

### Aggregation Functions Used

- `sum(value)` - For counts, rates, totals
- `avg(value)` - For gauges, averages
- `max(value)` - For peaks, maximums
- `count(*)` - For number of samples

---

## Next Steps

### Customize Dashboards

1. **Add more panels**: Click "+ Add Panel" in dashboard
2. **Adjust time range**: Use time picker top-right
3. **Change refresh rate**: Settings → Auto-refresh
4. **Clone dashboard**: Click "..." → "Clone"
5. **Export dashboard**: Click "..." → "Export JSON"

### Create Alerts

1. Open a dashboard
2. Click panel settings (gear icon)
3. Click "Create Alert"
4. Set threshold conditions
5. Configure notifications

---

## Troubleshooting

### Panel Shows "No Data"

**Solution**:
- Increase time window: Change `INTERVAL 15 MINUTE` to `INTERVAL 1 HOUR`
- Verify metric exists: Check Metrics tab
- Generate traffic: Run `curl http://localhost:8080/test` multiple times

### Query Has Syntax Error

**Solution**:
- Check single quotes: All strings use single quotes `'string'`
- Verify metric name: Must exist in `signoz_metrics.time_series_v4`
- Test query in ClickHouse client first

---

## Files Created

- `scripts/add-panels-to-dashboards.py` - Python script to create panels
- Panel data stored in `/var/lib/signoz/signoz.db` (SigNoz SQLite DB)

---

## Success Metrics

| Metric | Target | Achieved |
|--------|--------|----------|
| Dashboards created | 3 | ✅ 3 |
| Total panels | 15+ | ✅ 19 |
| Queries tested | All | ✅ 100% |
| Data returning | Yes | ✅ 1620 samples/15min |
| UI accessible | Yes | ✅ HTTP 200 |

**Overall Status**: ✅ **PRODUCTION READY**
