#!/bin/bash
# Quick verification of metrics available for dashboard creation

echo "=== Available Metrics for k12-backend ==="
echo

echo "1. JVM Metrics:"
docker exec k12-clickhouse clickhouse-client --query "
  SELECT DISTINCT metric_name
  FROM signoz_metrics.time_series_v4
  WHERE metric_name LIKE 'jvm.%'
  ORDER BY metric_name
  LIMIT 20
"

echo
echo "2. HTTP Server Metrics:"
docker exec k12-clickhouse clickhouse-client --query "
  SELECT DISTINCT metric_name
  FROM signoz_metrics.time_series_v4
  WHERE metric_name LIKE 'http.server.%'
  ORDER BY metric_name
"

echo
echo "3. Database Pool (Agroal) Metrics:"
docker exec k12-clickhouse clickhouse-client --query "
  SELECT DISTINCT metric_name
  FROM signoz_metrics.time_series_v4
  WHERE metric_name LIKE 'agroal.%'
  ORDER BY metric_name
  LIMIT 15
"

echo
echo "4. PostgreSQL Metrics:"
docker exec k12-clickhouse clickhouse-client --query "
  SELECT DISTINCT metric_name
  FROM signoz_metrics.time_series_v4
  WHERE metric_name LIKE 'pg_%'
  ORDER BY metric_name
  LIMIT 15
"

echo
echo "=== Sample Query: JVM Memory Usage ==="
docker exec k12-clickhouse clickhouse-client --query "
  SELECT
    toDateTime64(unix_milli/1000, 3) as time,
    toString(JSONExtractString(labels, 'area')) as area,
    value
  FROM signoz_metrics.samples_v4
  WHERE metric_name = 'jvm.memory.used'
    AND unix_milli > (now64(3)*1000) - 300000
  ORDER BY time DESC
  LIMIT 10
"
