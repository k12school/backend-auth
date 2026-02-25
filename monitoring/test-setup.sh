#!/bin/bash
# Test script to verify monitoring stack

echo "========================================="
echo "Testing K12 Backend Monitoring Stack"
echo "========================================="
echo ""

# 1. Check Quarkus metrics endpoint
echo "1. Checking Quarkus metrics endpoint..."
if curl -s http://localhost:8080/metrics > /dev/null; then
    REQUEST_COUNT=$(curl -s http://localhost:8080/metrics | grep "^http_server_requests_seconds_count" | wc -l)
    echo "   ✅ Metrics endpoint accessible ($REQUEST_COUNT HTTP metrics found)"
else
    echo "   ❌ Metrics endpoint not accessible - restart Quarkus with new config"
fi

# 2. Generate some test traffic
echo ""
echo "2. Generating test traffic..."
for i in {1..10}; do
    curl -s http://localhost:8080/q/health > /dev/null 2>&1
done
echo "   ✅ Generated 10 health check requests"

# 3. Check Prometheus targets
echo ""
echo "3. Checking Prometheus targets..."
BACKEND_STATUS=$(curl -s http://localhost:9090/api/v1/targets 2>/dev/null | \
    jq -r '.data.activeTargets[] | select(.labels.job=="k12-backend") | .health' 2>/dev/null)

if [ "$BACKEND_STATUS" = "up" ]; then
    echo "   ✅ k12-backend target is UP in Prometheus"
else
    echo "   ⚠️  k12-backend target status: $BACKEND_STATUS"
    echo "      This means Quarkus needs to be restarted to bind to 0.0.0.0"
fi

# 4. Check for HTTP metrics in Prometheus
echo ""
echo "4. Querying metrics from Prometheus..."
HTTP_METRICS=$(curl -s 'http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count' 2>/dev/null | \
    jq -r '.data.result | length')

if [ "$HTTP_METRICS" -gt 0 ]; then
    echo "   ✅ Found $HTTP_METRICS HTTP metric series in Prometheus"
    echo ""
    echo "   Sample metrics:"
    curl -s 'http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count' 2>/dev/null | \
        jq -r '.data.result[] | "     - \(.metric.method) \(.metric.uri) (\(.metric.status)): \(.value[1]) requests"' | head -3
else
    echo "   ❌ No HTTP metrics found in Prometheus"
fi

# 5. Check JVM metrics
echo ""
echo "5. Checking JVM metrics..."
JVM_METRICS=$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes' 2>/dev/null | \
    jq -r '.data.result | length')

if [ "$JVM_METRICS" -gt 0 ]; then
    echo "   ✅ Found $JVM_METRICS JVM memory metric series"
else
    echo "   ❌ No JVM metrics found"
fi

# 6. Check Loki logs
echo ""
echo "6. Checking log collection..."
LOG_COUNT=$(curl -s 'http://localhost:3100/loki/api/v1/query' --data-urlencode 'query={job="k12-backend"}' 2>/dev/null | \
    jq -r '.data.stats.result | length // 0' 2>/dev/null)

if [ "$LOG_COUNT" -gt 0 ]; then
    echo "   ✅ Logs are being collected in Loki"
else
    echo "   ⚠️  No logs found in Loki (may need traffic first)"
fi

# 7. Summary
echo ""
echo "========================================="
echo "Access URLs:"
echo "========================================="
echo "  Grafana:      http://localhost:3000 (admin/admin)"
echo "  Prometheus:   http://localhost:9090"
echo "  Quarkus:      http://localhost:8080"
echo ""
echo "To restart Quarkus with new config:"
echo "  1. Stop current: Ctrl+C in terminal running quarkusDev"
echo "  2. Restart: ./gradlew quarkusDev"
echo ""
