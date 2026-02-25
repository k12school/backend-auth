#!/bin/bash
echo "========================================="
echo "GRAFANA DASHBOARDS OVERVIEW"
echo "========================================="
echo ""

echo "📊 Available Dashboards:"
echo ""
echo "1. K12 Backend - JVM Metrics"
echo "   Purpose: Monitor Java Virtual Machine health"
echo "   Panels:"
echo "   ✓ JVM Heap Memory (by pool: Eden, Old Gen, Survivor)"
echo "   ✓ Heap Usage % (gauge)"
echo "   ✓ GC Pause Time Rate"
echo "   ✓ GC Count Rate"
echo ""

echo "2. K12 Backend - HTTP Metrics"
echo "   Purpose: Monitor HTTP request performance"
echo "   Panels:"
echo "   ✓ Request Rate (by endpoint, status code)"
echo "   ✓ Average Request Latency"
echo "   ✓ Request Rate by Status (stat with colors)"
echo "   ✓ Requests by Status Code (pie chart)"
echo ""

echo "3. K12 Backend - Distributed Traces"
echo "   Purpose: View and analyze request traces"
echo "   Panels:"
echo "   ✓ Trace Browser (search, filter, timeline view)"
echo "   ✓ Links to logs and metrics"
echo ""

echo "4. K12 Backend - PostgreSQL Performance"
echo "   Purpose: Monitor database health and queries"
echo "   Panels:"
echo "   ✓ Active Database Connections"
echo "   ✓ Cache Hit vs Read (Block I/O)"
echo "   ✓ Query Execution Time Distribution"
echo "   ✓ Slowest Query (Avg Execution Time)"
echo ""

echo "========================================="
echo "HOW TO ACCESS:"
echo "========================================="
echo ""
echo "1. Open: http://localhost:3000"
echo "2. Login: admin / admin"
echo "3. Click: Dashboards (sidebar icon with 4 squares)"
echo "4. Browse: K12 Backend folder"
echo "5. Select: Any dashboard"
echo ""

echo "========================================="
echo "SAMPLE METRICS CURRENTLY:"
echo "========================================="
echo ""

# Get some current metrics
echo "HTTP Metrics:"
curl -s 'http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count' 2>/dev/null | \
  jq -r '.data.result[] | "  - \(.metric.method) \(.metric.uri) (\(.metric.status)): \(.value[1] | tonumber) requests"' 2>/dev/null || echo "  No HTTP metrics yet"

echo ""
echo "JVM Memory:"
curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{area="heap",id="G1 Old Gen"}' 2>/dev/null | \
  jq -r '"  - Old Gen Heap: \(.value[1] | tonumber / 1024 / 1024 | floor) MB"' 2>/dev/null || echo "  No JVM metrics yet"

echo ""
echo "Database Connections:"
curl -s 'http://localhost:9090/api/v1/query?query=pg_stat_database_numbackends' 2>/dev/null | \
  jq -r '.data.result[] | "  - \(.metric.datname): \(.value[1]) connections"' 2>/dev/null || echo "  No DB metrics yet"

echo ""
echo "========================================="
echo "For detailed docs: monitoring/README.md"
echo "========================================="
