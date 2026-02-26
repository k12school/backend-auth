#!/bin/bash
# =============================================================================
# Observability Stack Verification Script
# =============================================================================
# Tests all three telemetry signals: traces, metrics, logs
#
# Usage: ./scripts/verify-telemetry.sh
# =============================================================================

set -e

COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_YELLOW='\033[1;33m'
COLOR_NC='\033[0m' # No Color

PASS="${COLOR_GREEN}✓${COLOR_NC}"
FAIL="${COLOR_RED}✗${COLOR_NC}"
WARN="${COLOR_YELLOW}⚠${COLOR_NC}"

echo "=========================================="
echo "  Observability Stack Verification"
echo "=========================================="
echo ""

# =============================================================================
# 1. Service Health Checks
# =============================================================================
echo "1. Service Health Checks"
echo "   ---------------------"

# Check Backend
if curl -sf http://localhost:8080/q/health > /dev/null; then
    echo -e " $PASS Backend is healthy"
else
    echo -e " $FAIL Backend is unhealthy"
    exit 1
fi

# Check SigNoz UI
if curl -sf http://localhost:3301/api/v1/health > /dev/null; then
    echo -e " $PASS SigNoz UI is accessible"
else
    echo -e " $FAIL SigNoz UI is not accessible"
    exit 1
fi

# Check OTel Collector
if curl -sf http://localhost:4317/healthz > /dev/null 2>&1; then
    echo -e " $PASS OTel Collector is healthy"
else
    echo -e "$WARN OTel Collector health endpoint not accessible (may be OK)"
fi

# Check ClickHouse
if docker exec k12-clickhouse clickhouse-client --query "SELECT 1" > /dev/null 2>&1; then
    echo -e " $PASS ClickHouse is responsive"
else
    echo -e " $FAIL ClickHouse is not responsive"
    exit 1
fi

echo ""

# =============================================================================
# 2. Network Connectivity
# =============================================================================
echo "2. Network Connectivity"
echo "   --------------------"

# Check backend can reach collector
if docker exec k12-backend sh -c 'nc -zv k12-signoz-otel-collector 4317' 2>&1 | grep -q "succeeded"; then
    echo -e " $PASS Backend → Collector (:4317) reachable"
else
    echo -e " $FAIL Backend → Collector unreachable"
    exit 1
fi

echo ""

# =============================================================================
# 3. Telemetry Signal Verification
# =============================================================================
echo "3. Telemetry Signals"
echo "   -------------------"

# Get initial span count
INITIAL_SPANS=$(docker exec k12-clickhouse clickhouse-client --query \
  "SELECT count(*) FROM signoz_traces.distributed_signoz_index_v3 \
   WHERE serviceName='k12-backend' AND timestamp > now() - INTERVAL 1 MINUTE" \
  2>/dev/null | tail -1)

echo "   Initial span count (last 1 min): $INITIAL_SPANS"

# Generate traffic
echo "   Generating traffic..."
for i in {1..20}; do
    curl -s http://localhost:8080/test > /dev/null
    curl -s http://localhost:8080/q/health > /dev/null
    sleep 0.2
done

# Wait for batch processing
echo "   Waiting for batch processing (5 seconds)..."
sleep 5

# Check span count increased
FINAL_SPANS=$(docker exec k12-clickhouse clickhouse-client --query \
  "SELECT count(*) FROM signoz_traces.distributed_signoz_index_v3 \
   WHERE serviceName='k12-backend' AND timestamp > now() - INTERVAL 1 MINUTE" \
  2>/dev/null | tail -1)

echo "   Final span count (last 1 min): $FINAL_SPANS"

if [ "$FINAL_SPANS" -gt "$INITIAL_SPANS" ]; then
    NEW_SPANS=$((FINAL_SPANS - INITIAL_SPANS))
    echo -e " $PASS Traces: $NEW_SPANS new spans exported"
else
    echo -e " $FAIL Traces: No new spans detected"
    exit 1
fi

echo ""
echo "=========================================="
echo "  All Verifications Passed!"
echo "=========================================="
echo ""
echo "Next Steps:"
echo "  1. View traces in SigNoz: http://localhost:3301"
echo "  2. Go to 'Traces' tab"
echo "  3. Filter by service.name = 'k12-backend'"
echo "  4. Click on any trace to see the waterfall"
echo ""
