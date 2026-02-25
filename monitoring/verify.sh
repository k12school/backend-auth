#!/bin/bash
echo "========================================="
echo "K12 Backend Monitoring - Verification"
echo "========================================="
echo ""

# Test different metric categories
echo "✅ HTTP METRICS"
echo "-------------------------------------------"
curl -s 'http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count' | \
  jq -r '.data.result[] | "  \(.metric.method) \(.metric.uri): \(.value[1] | tonumber) requests"'
echo ""

echo "✅ JVM MEMORY"
echo "-------------------------------------------"
curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes' | \
  jq -r '.data.result[] | "  \(.metric.area)/\(.metric.id): \(.value[1] | tonumber / 1024 / 1024 | floor) MB"' | head -5
echo ""

echo "✅ THREADS"
echo "-------------------------------------------"
curl -s 'http://localhost:9090/api/v1/query?query=jvm_threads_live_threads' | \
  jq -r '.data.result[] | "  Live threads: \(.value[1] | tonumber)"'
echo ""

echo "✅ GC METRICS"
echo "-------------------------------------------"
curl -s 'http://localhost:9090/api/v1/query?query=jvm_gc_pause_seconds_count' | \
  jq -r '.data.result[] | "  GC pauses: \(.value[1] | tonumber)"'
echo ""

echo "✅ SYSTEM METRICS"
echo "-------------------------------------------"
curl -s 'http://localhost:9090/api/v1/query?query=system_cpu_usage' 2>/dev/null || \
  curl -s 'http://localhost:9090/api/v1/query?query=process_cpu_seconds_total' | \
  jq -r '.data.result[] | "  CPU time: \(.value[1] | tonumber) seconds"'
echo ""

echo "========================================="
echo "Access Grafana: http://localhost:3000"
echo "Login: admin / admin"
echo "========================================="
