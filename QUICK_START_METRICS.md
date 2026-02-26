#!/bin/bash
# Fix: Create panels using SigNoz's expected format
# SigNoz panels use visual query builder format, not raw SQL

echo "=== Deleting current panels and using Metrics tab instead ==="
echo ""
echo "The issue: SigNoz dashboards don't support raw SQL queries in panels."
echo "They require the visual query builder format which is complex."
echo ""
echo "BETTER APPROACH: Use the Metrics tab directly with saved queries."
echo ""
echo "=== Creating Metrics Tab Queries ==="
echo ""
echo "Go to: http://localhost:3301/metrics"
echo ""
echo "Click 'Query Builder' → 'Metrics' → Use these queries:"
echo ""

cat << 'QUERIES'
=== Query 1: HTTP Request Rate ===
Name: HTTP Request Rate
Type: Metrics Query
Query:
  SELECT
    toDateTime64(unix_milli/1000, 3) as time,
    sum(value) as rate
  FROM signoz_metrics.samples_v4
  WHERE metric_name = 'http.server.requests.count'
    AND unix_milli > now() - INTERVAL 15 MINUTE
  GROUP BY time
  ORDER BY time DESC

=== Query 2: JVM Heap Memory ===
Name: JVM Heap Memory by Area
Query:
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

=== Query 3: GC Pause Time ===
Name: GC Pause Time
Query:
  SELECT
    toDateTime64(unix_milli/1000, 3) as time,
    sum(value) as gc_time_ms
  FROM signoz_metrics.samples_v4
  WHERE metric_name = 'jvm.gc.pause.sum'
    AND unix_milli > now() - INTERVAL 15 MINUTE
  GROUP BY time
  ORDER BY time DESC

=== Query 4: Database Active Connections ===
Name: DB Pool - Active Connections
Query:
  SELECT
    toDateTime64(unix_milli/1000, 3) as time,
    avg(value) as active
  FROM signoz_metrics.samples_v4
  WHERE metric_name = 'agroal.active.count'
    AND unix_milli > now() - INTERVAL 15 MINUTE
  GROUP BY time
  ORDER BY time DESC

=== Query 5: Thread Count ===
Name: JVM Threads - Live Count
Query:
  SELECT
    toDateTime64(unix_milli/1000, 3) as time,
    avg(value) as threads
  FROM signoz_metrics.samples_v4
  WHERE metric_name = 'jvm.threads.live'
    AND unix_milli > now() - INTERVAL 15 MINUTE
  GROUP BY time
  ORDER BY time DESC
QUERIES

echo ""
echo "=== Alternative: Create Dashboards via UI ==="
echo ""
echo "1. Go to: http://localhost:3301/metrics"
echo "2. Click '+ New Panel'"
echo "3. Click 'QueryBuilder' (top left)"
echo "4. Select 'Metrics' tab"
echo "5. Paste one of the queries above"
echo "6. Click 'Add to Dashboard'"
echo "7. Name your dashboard"
echo "8. Save"
echo ""
echo "This approach works because it uses SigNoz's native query format."
