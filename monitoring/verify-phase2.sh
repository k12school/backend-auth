#!/bin/bash
# Phase 2 Monitoring Stack Verification

echo "========================================="
echo "Phase 2 Monitoring Stack Verification"
echo "========================================="
echo ""

# Check Tempo
echo "=== TEMPO (Distributed Tracing) ==="
if curl -s http://localhost:3200/metrics > /dev/null; then
    echo "✅ Tempo is running"
    TRACE_COUNT=$(curl -s "http://localhost:3200/api/search?limit=100" 2>/dev/null | jq '.traces | length // 0')
    echo "   Traces stored: $TRACE_COUNT"
else
    echo "❌ Tempo not accessible"
fi
echo ""

# Check Postgres Exporter
echo "=== POSTGRES EXPORTER ==="
if curl -s http://localhost:9187/metrics > /dev/null; then
    echo "✅ Postgres Exporter is running"
    METRICS=$(curl -s http://localhost:9187/metrics | grep "^pg_" | wc -l)
    echo "   Database metrics: $METRICS available"
    echo ""
    echo "   Sample metrics:"
    curl -s 'http://localhost:9090/api/v1/query?query=pg_stat_database_numbackends' 2>/dev/null | \
        jq -r '.data.result[] | "   - Connections: \(.value[1])"' | head -2
else
    echo "❌ Postgres Exporter not accessible"
fi
echo ""

# Check Uptime Kuma
echo "=== UPTIME KUMA ==="
if curl -s http://localhost:3001 > /dev/null; then
    echo "✅ Uptime Kuma is running"
    echo "   Access: http://localhost:3001"
    echo "   Note: First-time setup required (create admin account)"
else
    echo "❌ Uptime Kuma not accessible"
fi
echo ""

# Check Prometheus targets
echo "=== PROMETHEUS TARGETS ==="
curl -s http://localhost:9090/api/v1/targets | jq -r '.data.activeTargets[] |
    select(.labels.job | test("tempo|postgres")) |
    "   \(.labels.job): \(.health)"'
echo ""

# Generate test traffic
echo "=== GENERATING TEST TRAFFIC ==="
for i in {1}{b}; do
    curl -s http://localhost:8080/q/health > /dev/null 2>&1
done
echo "✅ Generated 20 requests (check traces in 30 seconds)"
echo ""

# Summary
echo "========================================="
echo "Access URLs:"
echo "========================================="
echo "  Grafana:        http://localhost:3000"
echo "  Tempo:          http://localhost:3200"
echo "  Postgres Exporter: http://localhost:9187/metrics"
echo "  Uptime Kuma:    http://localhost:3001"
echo ""
echo "Dashboards:"
echo "  - K12 Backend - Distributed Traces"
echo "  - K12 Backend - PostgreSQL Performance"
echo ""
echo "For setup guide: monitoring/PHASE2_COMPLETE.md"
echo "========================================="
