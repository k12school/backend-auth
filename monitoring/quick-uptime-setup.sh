#!/bin/bash
# Quick Uptime Kuma Setup Script

echo "========================================="
echo "Uptime Kuma - Quick Setup Guide"
echo "========================================="
echo ""

# Test connectivity
echo "Testing connectivity to host services..."
echo ""

# Test host.docker.internal
echo "1. Testing host.docker.internal:"
HEALTH_CODE=$(docker exec k12-uptime-kuma curl -s -o /dev/null -w "%{http_code}" http://host.docker.internal:8080/q/health 2>/dev/null)
METRICS_CODE=$(docker exec k12-uptime-kuma curl -s -o /dev/null -w "%{http_code}" http://host.docker.internal:8080/metrics 2>/dev/null)

if [ "$HEALTH_CODE" = "200" ]; then
    echo "   ✅ Health check: $HEALTH_CODE (OK)"
    USE_HOST_DOCKER="true"
else
    echo "   ❌ Health check: $HEALTH_CODE (FAILED)"
    USE_HOST_DOCKER="false"
fi

if [ "$METRICS_CODE" = "200" ]; then
    echo "   ✅ Metrics: $METRICS_CODE (OK)"
else
    echo "   ❌ Metrics: $METRICS_CODE (FAILED)"
fi

echo ""

# Test bridge gateway as alternative
echo "2. Testing Docker bridge gateway:"
GATEWAY_IP=$(docker network inspect bridge | jq -r '.[0].IPAM.Config[0].Gateway')
echo "   Gateway IP: $GATEWAY_IP"

GW_HEALTH_CODE=$(docker exec k12-uptime-kuma curl -s -o /dev/null -w "%{http_code}" http://${GATEWAY_IP}:8080/q/health 2>/dev/null)
GW_METRICS_CODE=$(docker exec k12-uptime-kuma curl -s -o /dev/null -w "%{http_code}" http://${GATEWAY_IP}:8080/metrics 2>/dev/null)

if [ "$GW_HEALTH_CODE" = "200" ]; then
    echo "   ✅ Health check: $GW_HEALTH_CODE (OK)"
    USE_GATEWAY="true"
else
    echo "   ❌ Health check: $GW_HEALTH_CODE (FAILED)"
    USE_GATEWAY="false"
fi

if [ "$GW_METRICS_CODE" = "200" ]; then
    echo "   ✅ Metrics: $GW_METRICS_CODE (OK)"
else
    echo "   ❌ Metrics: $GW_METRICS_CODE (FAILED)"
fi

echo ""
echo "========================================="
echo "Setup Instructions:"
echo "========================================="
echo ""

# Determine which method to recommend
if [ "$USE_HOST_DOCKER" = "true" ]; then
    HOST="host.docker.internal"
    echo "✅ Use: host.docker.internal"
    echo ""
else
    HOST="$GATEWAY_IP"
    echo "⚠️  host.docker.internal not working"
    echo "✅ Use: $GATEWAY_IP"
    echo ""
fi

echo "1. Open Uptime Kuma:"
echo "   http://localhost:3001"
echo ""

if [ ! -f "/tmp/uptime-kuma-setup.txt" ]; then
    echo "2. Create admin account (first time only)"
    echo ""

    echo "3. Add these monitors:"
    echo ""
    echo "   Monitor 1: K12 Backend - Health"
    echo "   - Type: HTTP"
    echo "   - URL: http://$HOST:8080/q/health"
    echo "   - Interval: 1 minute"
    echo "   - Keyword: UP"
    echo ""
    echo "   Monitor 2: K12 Backend - Metrics"
    echo "   - Type: HTTP"
    echo "   - URL: http://$HOST:8080/metrics"
    echo "   - Interval: 2 minutes"
    echo ""
    echo "   Monitor 3: PostgreSQL"
    echo "   - Type: TCP Port"
    echo "   - Hostname: $HOST"
    echo "   - Port: 15432"
    echo "   - Interval: 1 minute"
    echo ""
    echo "   Monitor 4: Grafana"
    echo "   - Type: HTTP"
    echo "   - URL: http://$HOST:3000"
    echo "   - Interval: 5 minutes"
    echo ""
    echo "   Monitor 5: Prometheus"
    echo "   - Type: HTTP"
    echo "   - URL: http://$HOST:9090"
    echo "   - Interval: 5 minutes"
    echo ""
fi

echo "========================================="
echo "For detailed instructions:"
echo "   monitoring/UPTIME_KUMA_SETUP.md"
echo "========================================="

# Create marker to show this was run
touch /tmp/uptime-kuma-setup.txt
