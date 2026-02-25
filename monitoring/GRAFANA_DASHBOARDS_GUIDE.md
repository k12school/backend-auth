# 📊 Grafana Dashboards - Visual Guide

## Access & Navigation

```
┌─────────────────────────────────────────────────────┐
│  Grafana                                            │
│  ┌─────────────────────────────────────────────┐    │
│  │ Search dashboards...                    │    │
│  └─────────────────────────────────────────────┘    │
│                                                      │
│  Dashboards         Explore         +          │
│  (4 squares)        (compass)       │          │
│                                                      │
│  ├─ K12 Backend                      Alerting   │
│  │   ├─ JVM Metrics                  ┌──────┐  │
│  │   ├─ HTTP Metrics                 │ 3 UP │  │
│  │   ├─ Distributed Traces           └──────┘  │
│  │   └─ PostgreSQL Performance                  │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## Dashboard 1: JVM Metrics

```
┌───────────────────────────────────────────────────────────────┐
│ K12 Backend - JVM Metrics                   [Last 1 hour] [▶]   │
├────────────────────────────────────────────────────────────────│
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ JVM Heap Memory (Bytes)                     [Legend ▼]│   │
│  │ 180M ┤                                               │   │
│  │ 160M ┤  ╭╮                                          │   │
│  │ 140M ┤  ││  ╭─╮                                      │   │
│  │ 120M ┤  ││  │ │ ╭─╮                                  │   │
│  │ 100M ┤  ││  │ │ │ │ ╭─╮                               │   │
│  │  80M ┤  ││  │ │ │ │ │ │ ╭──╮                           │   │
│  │  60M ┤  ││  │ │ │ │ │ │ │  │                           │   │
│  │      └──┴┴──┴┴──┴┴──┴┴──┴──┴──┴────────────────          │   │
│  │       Old Gen    Eden      Survivor    Metaspace           │   │
│  │       (used)     (used)     (used)      (used)            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Heap Usage %                            Gauge           │   │
│  │         ╭─╮                                          │   │
│  │        ╭──╯ │                                          │   │
│  │       ╭─╯    │        ████████████                       │   │
│  │      ╭─╯      │        ████████████████                   │   │
│  │     ╭─╯       ████    ████████████████████                │   │
│  │    ╭─╯         ████    ████████████████████████           │   │
│  │                 0%    25%   50%   75%  100%                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                               │
│  GC Pause Rate        GC Count Rate                           │
│  ╭──╮                  ████                                    │
│  │  │ 0.1/s          ████ ████                                │
│  └──┴               ████ ████ ████                             │
└───────────────────────────────────────────────────────────────────┘
```

**What you're seeing:**
- **Memory pools**: G1 Eden, G1 Old Gen, G1 Survivor Space, Metaspace
- **Heap gauge**: Current heap utilization (green = good, red = high)
- **GC metrics**: How often garbage collection runs and pause times

---

## Dashboard 2: HTTP Metrics

```
┌─────────────────────────────────────────────────────────────────────┐
│ K12 Backend - HTTP Metrics                    [Last 1 hour] [▶]      │
├────────────────────────────────────────────────────────────────────│
│                                                                      │
│  Request Rate (requests/sec)                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 50 ┤                                            ┌─┐          │    │
│  │ 40 ┤                                  ┌─┐ ┌─┐          │    │
│  │ 30 ┤                    ┌─┐          │ │ │ │          │    │
│  │ 20 ┤          ┌─┐      │ │          │ │ │ │          │    │
│  │ 10 ┤    ┌─┐   │ │      │ │          │ │ │ │          │    │
│  │  0 ┼────┴─────┴───────┴────┴─────────────┴───┴───────┘     │    │
│  │        10:00    10:05    10:10    10:15    10:20          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                   GET /metrics (200)                              │
│                                                                      │
│  Average Latency (ms)                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 100 ┤                                                       │    │
│  │  75 ┤              ┌───┐                                     │    │
│  │  50 ┤         ┌────┘   │                                     │    │
│  │  25 ┤    ┌────┘        │                                     │    │
│  │   0 ┼────┴─────────────┴────────────────────────            │    │
│  │        10:00    10:05    10:10    10:15                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                   GET /q/health (200)                            │
│                                                                      │
│  Request Rate by Status                        Requests by Status       │
│  ┌─────────────┐                             ┌─────────────────┐    │
│  │   52 req/s  │                             │     ●           │    │
│  │   [████████]│                             │    ●●●         │    │
│  │   2xx (green)│                             │  ●●●●●●●●       │    │
│  └─────────────┘                             └─────────────────┘    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**What you're seeing:**
- **Request rate**: How many requests per second by endpoint
- **Latency**: Average response time
- **Status breakdown**: Success (2xx), Redirects (3xx), Client errors (4xx), Server errors (5xx)

---

## Dashboard 3: Distributed Traces

```
┌─────────────────────────────────────────────────────────────────────┐
│ K12 Backend - Distributed Traces                 [Last 1 hour] [▶]   │
├────────────────────────────────────────────────────────────────────│
│                                                                      │
│  Search: { service.name = "k12-backend" }           [Search 🔍]     │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ Filter                                                       │ │
│  │ Min Duration: [      ] Max Duration: [      ]               │ │
│  │ Tags: [http.method: GET] [http.status_code: 200]            │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Traces (showing 20 of 150)                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ Trace ID       │ Service   │ Duration │ Root Span      │Act│   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │ abc123def456  │ k12-b...  │ 45ms     │ GET /q/health │   │   │
│  │ 789ghi012jkl  │ k12-b...  │ 128ms    │ POST /api/... │   │   │
│  │ mno345pqr678  │ k12-b...  │ 23ms     │ GET /metrics │   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ Trace: abc123def456 - Total: 45ms                          │   │
│  │                                                             │   │
│  │ HTTP GET /q/health ─────────────────────● 45ms               │   │
│  │   └─ JDBC SELECT ─────────────● 12ms                          │   │
│  │   └─ JWT Validation ─────────● 3ms                            │   │
│  │                                                             │   │
│  │ Attributes:                                                  │   │
│  │   http.method: GET                                          │   │
│  │   http.status_code: 200                                      │   │
│  │   http.route: /q/health                                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  [← Previous]            [View in Tempo]            [Next →]       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**What you're seeing:**
- **Trace list**: All traces with IDs, service name, duration
- **Trace timeline**: Visual breakdown of where time was spent
- **Span details**: Click any span to see logs, tags, attributes
- **Links**: Jump to Tempo for detailed trace view

---

## Dashboard 4: PostgreSQL Performance

```
┌─────────────────────────────────────────────────────────────────────┐
│ K12 Backend - PostgreSQL Performance            [Last 1 hour] [▶]   │
├────────────────────────────────────────────────────────────────────│
│                                                                      │
│  Active Database Connections                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 5 ┤                                               ╭─╮         │    │
│  │ 4 ┤                                  ╭───╯                │    │
│  │ 3 ┤                    ╭─╮                             │    │
│  │ 2 ┤          ╭─╮     │ │                             │    │
│  │ 1 ┤    ╭─╮   │ │     │ │                             │    │
│  │ 0 ┼────┴───────┴─────┴────────────────                   │    │
│  │      10:00    10:05    10:10    10:15                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                   k12_db                                          │
│                                                                      │
│  Cache Hit vs Read (Block I/O)                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 10K ┤                       ╭──╮                             │    │
│  │     │                    ╭──╯ ╭─╮                            │    │
│  │ 5K  ┤               ╭──╯ ╭──╯ ╭─╮                           │    │
│  │     │          ╭──╯ ╭──╯ │ │ ╭─╮                          │    │
│  │  0  ┼──────────┴─────┴────┴─┴──┴                        │    │
│  │        10:00  10:05  10:10  10:15                         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                   Hits (blue)       Reads (orange)               │    │
│                                                                      │
│  Query Execution Time Distribution      Slowest Query               │
│  ┌─────────────────────────┐            ┌─────────────────┐       │
│  │     ╭─╮                │            │     ████        │       │
│  │    ╭──╯ ╭─╮            │            │    ██████       │       │
│  │   ╭──╯ ╭──╯ ╭─╮        │            │   ████████      │       │
│  │  ╭──╯ ╭──╯ ╭──╯        │            │  145.3 ms       │       │
│  │ ┴────┴────┴─────        │            │                 │       │
│  │   1ms  10ms  100ms  1s  │            │ Query: SELECT   │       │
│  └─────────────────────────┘            │ FROM users...   │       │
│  │   Most queries         │            │                 │       │
│  └─────────────────────────┘            └─────────────────┘       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**What you're seeing:**
- **Connections**: How many active database connections
- **Cache performance**: Blue = cache hits (good), Orange = disk reads (slow)
- **Query histogram**: Distribution of query execution times
- **Slowest query**: Gauge showing worst-performing query type

---

## Quick Start Guide

### 1. Open Grafana
```
http://localhost:3000
Login: admin / admin
```

### 2. Navigate to Dashboards
```
Dashboards → Browse → K12 Backend → Select dashboard
```

### 3. Interact with Panels

- **Hover**: See exact values
- **Click legend**: Toggle series
- **Drag**: Zoom into time range
- **Double-click**: Reset zoom
- **⋯ menu**: Export, share, inspect

### 4. Common Queries

In **HTTP Metrics** dashboard:
- Look for spikes in request rate
- Check latency trends
- Identify any 5xx errors (red)

In **JVM Metrics** dashboard:
- Monitor heap usage trend
- Check GC frequency (spikes = pressure)
- Watch thread count

In **PostgreSQL** dashboard:
- Check connection pool utilization
- Monitor cache hit ratio (should be > 90%)
- Find slow queries

In **Distributed Traces**:
- Search: `{ duration > 1000ms }` for slow requests
- Click trace ID to see full timeline
- Use "View in Tempo" for detailed analysis

---

## Pro Tips

1. **Set up Alerts**
   - Click ⚙️ (gear icon) on any panel
   - Set alert conditions (e.g., "Heap > 90%")
   - Configure notifications

2. **Customize Time Range**
   - Click time selector (top right)
   - Choose: Last 5m, 1h, 6h, 1d, etc.
   - Or set custom range

3. **Variables**
   - Dashboard variables (top dropdown)
   - Filter by: instance, job, tenant_id
   - Auto-populate from Prometheus

4. **Compare Time Ranges**
   - Enable "Time Shift" in panel options
   - Compare today vs yesterday
   - Spot patterns

5. **Save Favorites**
   - Organize frequently used dashboards
   - Star dashboards (⭐)
   - Create folders for teams

---

## Dashboard Files

All dashboards are stored in:
```
monitoring/grafana/dashboards/
├── quarkus-jvm-dashboard.json
├── http-metrics-dashboard.json
├── tempo-traces-dashboard.json
└── postgres-dashboard.json
```

You can edit, export, or import dashboards as JSON.

---

## Next Steps

1. **Explore**: Click through all 4 dashboards
2. **Customize**: Adjust panel settings to your needs
3. **Add alerts**: Set up notification channels
4. **Share**: Export dashboards for your team

Enjoy your full observability stack! 🚀
