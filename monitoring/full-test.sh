#!/bin/bash
echo "========================================="
echo "K12 Backend - Full Monitoring Test"
echo "========================================="
echo ""

# Test Phase 1
echo "PHASE 1: Metrics & Logs"
echo "----------------------------"
# HTTP Metrics
HTTP_COUNT=$(curl -s 'http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count' | jq -r '.data.result | length')
echo "✓ HTTP Metrics: $HTTP_COUNT series"

# JVM Metrics  
JVM_COUNT=$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes' | jq -r '.data.result | length')
echo "✓ JVM Metrics: $JVM_COUNT series"

echo ""

# Test Phase 2
echo "PHASE 2: Traces & Database"
echo "----------------------------"
# Tempo
if curl -s http://localhost:3200/metrics > /dev/null; then
    echo "✓ Tempo: Running"
else
    echo "✗ Tempo: Not accessible"
fi

# Postgres Exporter
PG_METRICS=$(curl -s http://localhost:9187/metrics | grep "^pg_stat" | wc -l)
echo "✓ Postgres Exporter: $PG_METRICS database metrics"

# Uptime Kuma
if curl -s http://localhost:3001 > /dev/null; then
    echo "✓ Uptime Kuma: Running"
else
    echo "✗ Uptime Kuma: Not accessible"
fi

echo ""
echo "========================================="
echo "Access Points:"
echo "========================================="
echo "Grafana:        http://localhost:3000"
echo "Prometheus:     http://localhost:9090"
echo "Tempo:          http://localhost:3200"
echo "Uptime Kuma:    http://localhost:3001"
echo ""
echo "========================================="
echo "Dashboards Available in Grafana:"
echo "========================================="
echo "- K12 Backend - JVM Metrics"
echo "- K12 Backend - HTTP Metrics"
echo "- K12 Backend - Distributed Traces"
echo "- K12 Backend - PostgreSQL Performance"
echo ""
