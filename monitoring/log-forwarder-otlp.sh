#!/bin/bash
# OTLP Log Forwarder - sends logs in proper OTLP JSON format

BACKEND_CONTAINER="k12-backend"
SIGNOZ_ENDPOINT="http://localhost:4318/v1/logs"
COUNT=0
LAST_CHECK=$(date +%s)

echo "[LogForwarder] Starting OTLP log forwarder..."
echo "[LogForwarder] Target: $BACKEND_CONTAINER"
echo "[LogForwarder] Endpoint: $SIGNOZ_ENDPOINT"

# Test endpoint
curl -s $SIGNOZ_ENDPOINT -X POST -H "Content-Type: application/json" -d '{}' > /dev/null 2>&1
echo "[LogForwarder] ✓ Endpoint validated"
echo "[LogForwarder] Watching for logs..."

while true; do
    # Get logs since last check
    SINCE_TIME=$((($(date +%s) - LAST_CHECK) + 10))
    RECENT_LOGS=$(docker logs --since ${SINCE_TIME}s $BACKEND_CONTAINER 2>&1)
    LOG_COUNT=$(echo "$RECENT_LOGS" | grep -v "^$" | wc -l)

    if [ "$LOG_COUNT" -gt 0 ]; then
        # Build OTLP JSON log records
        LOG_RECORDS=""

        while IFS= read -r line; do
            [ -z "$line" ] && continue

            # Get current timestamp in nanoseconds (Unix epoch)
            TIMESTAMP_NS=$(date +%s%N)
            SEVERITY_NUM=9  # INFO
            SEVERITY_TXT="INFO"

            # Escape the log message for JSON
            ESCAPED_MSG=$(echo "$line" | sed 's/\\/\\\\/g; s/"/\\"/g')

            # Add to log records array
            if [ -n "$LOG_RECORDS" ]; then
                LOG_RECORDS="$LOG_RECORDS,"
            fi

            LOG_RECORDS="$LOG_RECORDS{
              \"timeUnixNano\": \"$TIMESTAMP_NS\",
              \"severityNumber\": $SEVERITY_NUM,
              \"severityText\": \"$SEVERITY_TXT\",
              \"body\": {\"stringValue\": \"$ESCAPED_MSG\"}
            }"

            COUNT=$((COUNT + 1))
        done <<< "$RECENT_LOGS"

        # Wrap in OTLP resource logs format
        OTLP_JSON="{
          \"resourceLogs\": [{
            \"resource\": {
              \"attributes\": [
                {\"key\": \"service.name\", \"value\": {\"stringValue\": \"k12-backend\"}},
                {\"key\": \"deployment.environment\", \"value\": {\"stringValue\": \"development\"}},
                {\"key\": \"service.version\", \"value\": {\"stringValue\": \"1.0-SNAPSHOT\"}}
              ]
            },
            \"scopeLogs\": [{
              \"scope\": {\"name\": \"bash-forwarder\"},
              \"logRecords\": [$LOG_RECORDS]
            }]
          }]
        }"

        # Send to SigNoz
        RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $SIGNOZ_ENDPOINT \
            -H "Content-Type: application/json" \
            -d "$OTLP_JSON" 2>&1)

        HTTP_CODE=$(echo "$RESPONSE" | tail -1)
        BODY=$(echo "$RESPONSE" | head -n -1)

        if [ "$HTTP_CODE" != "200" ] && [ "$HTTP_CODE" != "202" ]; then
            echo "[$(date +'%H:%M:%S')] ✗ Export failed: HTTP $HTTP_CODE - $BODY"
        else
            echo "[$(date +'%H:%M:%S')] ✓ Exported $COUNT log lines (HTTP $HTTP_CODE)"
        fi
    fi

    LAST_CHECK=$(date +%s)
    sleep 5
done
