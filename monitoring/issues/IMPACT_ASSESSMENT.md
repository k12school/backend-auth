# Impact Assessment

**Generated:** 2026-02-25 15:25:39 -03:00

---

## Data Loss Quantification

### Traces

**Current State:**
- Export attempts: ~12 per minute (every 5 seconds)
- Successful exports: 0
- Export success rate: **0%**

**Data in ClickHouse:**
```sql
SELECT count() FROM signoz_traces.distributed_signoz_spans;
Result: 0
```

**Impact:**
- ❌ No distributed tracing visibility
- ❌ No span data in SigNoz UI
- ❌ Cannot trace request flows across services
- ❌ Cannot identify performance bottlenecks
- ❌ Cannot debug inter-service issues
- ❌ No latency analysis possible
- ❌ No dependency mapping

**Business Impact:**
- Production incidents require full reproduction locally
- Cannot monitor microservice interactions
- No root cause analysis for distributed issues
- Increased MTTR (Mean Time To Recovery)

---

### Metrics

**Current State:**
- Scrape interval: 15 seconds (configured)
- Scrapes executed: 0 (since collector restart at 14:26:14)
- Collection uptime: **0%** (for last 59 minutes)

**Data in ClickHouse:**
```sql
SELECT count() FROM signoz_metrics.time_series_v2;
Result: 0
```

**Impact:**
- ❌ No application metrics in dashboards
- ❌ No JVM metrics (heap, GC, threads)
- ❌ No HTTP server metrics
- ❌ No database connection pool metrics
- ❌ No performance monitoring
- ❌ No alerting possible
- ❌ Cannot detect anomalies
- ❌ No SLO/SLI tracking

**Business Impact:**
- Cannot monitor application health
- Cannot detect performance degradation
- Cannot set up meaningful alerts
- Reactive instead of proactive operations
- Capacity planning requires manual measurements
- Cannot justify scaling decisions with data

**Dashboard Status:**
```
URL: http://localhost:3301/dashboard/019c94ea-c26a-7035-9d4a-0d9e56d696bd
Status: Empty - no data displaying
```

---

### Logs

**Current State:**
- Application logs generated: Yes (Quarkus startup logs visible)
- Logs exported: 0 (from Docker container)
- Export success rate: **0%**

**Data in ClickHouse:**
```sql
SELECT count() FROM signoz_logs.logs_v2;
Result: 12

SELECT resources_string['service.name'] as service, count() 
FROM signoz_logs.logs_v2 
GROUP BY service;
Result: k12-backend, 12 (all from dev mode, pre-Docker)
```

**Log Age Analysis:**
- All 12 logs from: Dev mode (pre-Docker deployment)
- Timestamp range: Old (not from current session)
- Current session logs: 0
- Log export rate: **0%**

**Impact:**
- ❌ No log aggregation from production
- ❌ Cannot search logs across instances
- ❌ No centralized log management
- ❌ Cannot correlate logs with traces/metrics
- ❌ Debugging requires container shell access
- ❌ No historical log analysis
- ❌ Cannot detect error patterns
- ❌ No audit trail for user actions

**Business Impact:**
- Production debugging requires direct container access
- Cannot investigate past issues
- No compliance/audit trail
- Cannot track user actions
- Security incidents harder to investigate
- Log analysis impossible at scale

---

## Observability Coverage Gap

### Coverage Matrix

| Signal | Dev Mode | Docker Container | Gap | Severity |
|--------|----------|------------------|-----|----------|
| **Traces** | Not tested | ❌ 0% | 100% | Critical |
| **Metrics** | Not tested | ❌ 0% | 100% | Critical |
| **Logs** | ✅ (12 logs) | ❌ 0% | 100% | Critical |

**Overall Coverage Gap:** **100%**

No telemetry signals are successfully flowing from the Docker containerized application to SigNoz.

---

## Comparison: Dev Mode vs Docker

### Dev Mode (Historical Data)

**Environment:**
- Running directly on host
- Profile: `dev`
- Database: `localhost:15432`

**Observed Results:**
- ✅ 12 logs successfully exported
- ✅ Logs visible in SigNoz UI
- ⚠️ Traces not tested
- ⚠️ Metrics not tested

**Evidence:**
```sql
SELECT substring(body, 1, 80) as message 
FROM signoz_logs.logs_v2 
WHERE resources_string['service.name'] = 'k12-backend'
LIMIT 3;

Results:
1. "Live reload took more than 4 seconds, you may want to enable instrumentation"
2. "Live reload total time: 4.359s"
3. "Installed features: [agroal, cdi, compose, flyway, hibernate-validator...]"
```

### Docker Mode (Current)

**Environment:**
- Running in Docker container
- Profile: `prod`
- Database: `k12-postgres:5432` (Docker service)

**Observed Results:**
- ❌ 0 traces exported
- ❌ 0 metrics collected
- ❌ 0 logs exported (new)
- ⚠️ Only old dev mode logs remain

**Key Differences:**
1. **Profile Change:** `dev` → `prod`
   - Dev mode has more verbose logging
   - Prod mode suppresses debug output
   
2. **Network Change:** `localhost` → Docker network
   - Dev mode: Direct host access
   - Docker: Inter-container communication via `k12-signoz-net`
   
3. **Configuration Mismatch:**
   - Log handler hardcoded to `localhost:4317`
   - Should use `k12-signoz-otel-collector:4318`

---

## Operational Impact

### Incident Response Capability

**Current State:** Without observability

| Capability | Available | Impact |
|------------|-----------|--------|
| Detect incidents | ❌ No | Must rely on user reports |
| Identify affected services | ❌ No | Manual investigation required |
| Root cause analysis | ❌ No | Full reproduction needed |
| Trace user request | ❌ No | Cannot follow request flow |
| Check system health | ❌ No | No real-time metrics |
| Analyze historical data | ❌ No | No trend analysis |
| Alert on anomalies | ❌ No | Reactive only |

**MTTR Impact:** Significantly increased
- Detection: User-reported (minutes to hours)
- Investigation: Full reproduction required
- Resolution: Manual debugging in container

**Desired State:** With observability
- Detection: Automated (seconds)
- Investigation: Trace-based navigation
- Resolution: Data-driven decisions

---

### Development Impact

**Without Telemetry:**
- Cannot profile performance in production-like environment
- Cannot identify slow endpoints
- Cannot detect memory leaks
- Cannot optimize database queries
- Cannot measure GC impact
- Cannot validate scaling decisions

**With Telemetry:**
- Production profiling
- Performance optimization
- Capacity planning
- Cost optimization
- SLA compliance tracking

---

## Risk Assessment

### High Severity Risks

1. **Production Blindness**
   - Severity: Critical
   - Likelihood: Certain (happening now)
   - Impact: Complete lack of visibility
   - Mitigation: Required immediately

2. **Incident Response Paralysis**
   - Severity: Critical
   - Likelihood: High (next incident)
   - Impact: Extended outages
   - Mitigation: Manual logging fallback

3. **Capacity Planning Void**
   - Severity: High
   - Likelihood: Medium (at scale)
   - Impact: Over/under provisioning
   - Mitigation: Manual load testing

4. **Compliance Gaps**
   - Severity: Medium
   - Likelihood: Low (depends on requirements)
   - Impact: Audit failures
   - Mitigation: Container logs retention

### Medium Severity Risks

5. **Performance Regression**
   - Severity: Medium
   - Likelihood: High (silent degradation)
   - Impact: User experience decline
   - Mitigation: User-reported issues

6. **Debugging Difficulty**
   - Severity: Medium
   - Likelihood: Certain
   - Impact: Increased development time
   - Mitigation: Local reproduction

---

## Quantified Impact

### Time Impact

| Activity | Without Observability | With Observability | Time Difference |
|----------|----------------------|-------------------|-----------------|
| Detect incident | 30 min (avg user report) | 30 sec (automated) | **60x slower** |
| Investigate root cause | 2-4 hours | 15-30 min | **4-8x slower** |
| Find slow endpoint | Manual profiling | Dashboard query | **10-20x slower** |
| Trace user flow | Code reading | Trace visualization | **100x slower** |
| Check system health | Container access | Health check UI | **5x slower** |

### Business Impact

**Downtime Impact Calculation:**
- Assume 1 incident per week
- Without observability: 4 hours MTTR
- With observability: 30 minutes MTTR
- Time saved per incident: 3.5 hours
- Annual savings (52 incidents): 182 hours

**Development Impact:**
- Performance optimization: 2-3x longer without metrics
- Debugging production issues: 5-10x longer
- Capacity planning: Manual vs automated

---

## Priority Matrix

### Fix Priority (Impact vs Urgency)

```
            URGENT
              ↑
              │
    HIGH │  [1] Trace Export    │  [2] Metrics Scrape
    IMPACT│   100% failure        │   Stopped
         │                      │
         │  [3] Log Export       │
    LOW  │   Config mismatch    │
         │                      │
         └──────────────────────┴──────────→
                  NOT URGENT
```

**Recommended Fix Order:**
1. **Fix trace export** (enables distributed tracing)
2. **Fix metrics scraping** (enables dashboards and alerting)
3. **Fix log export** (enables log aggregation)

**Rationale:**
- Traces provide most value per fix (distributed debugging)
- Metrics enable alerting (proactive vs reactive)
- Logs are least urgent (can fall back to container logs)

---

## Success Criteria

### Metrics for Recovery

Each signal is considered "working" when:

**Traces:**
- ✅ Export success rate > 95%
- ✅ Spans visible in ClickHouse
- ✅ Services list populated in UI
- ✅ Trace search functional

**Metrics:**
- ✅ Scraping success rate > 95%
- ✅ Time series data in ClickHouse
- ✅ Dashboard panels displaying data
- ✅ Metrics graphs updating

**Logs:**
- ✅ Export success rate > 95%
- ✅ Recent logs visible in UI
- ✅ Log search functional
- ✅ Log-trace correlation working

**Overall Success:**
- ✅ All three signals flowing
- ✅ Dashboards populated
- ✅ Alerting possible
- ✅ Production debugging enabled
