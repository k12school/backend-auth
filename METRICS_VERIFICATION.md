# Metrics Verification Report

**Date**: 2026-02-26
**Status**: In Progress

## Test Setup

### Configuration
```properties
quarkus.otel.metrics.enabled=true
quarkus.otel.exporter.otlp.endpoint=http://k12-signoz-otel-collector:4317
```

### Traffic Generated
- 300 requests (150 x `/test` + `/q/health`)
- Waited 65 seconds for metrics export interval
- Generated additional 200 requests

## Current Status

### ✅ Traces - CONFIRMED WORKING
- **15,546+ total spans** in ClickHouse
- **39 new spans** after test run
- Automatic HTTP/JDBC instrumentation confirmed

### ❓ Metrics - INVESTIGATING

#### Finding 1: Old Metrics in ClickHouse
- Found 36,660 time series in `signoz_metrics.time_series_v4`
- Some tagged with `service.name=k12-backend`
- **BUT**: Scope shows `prometheusreceiver` (not OTLP!)

#### Finding 2: No Prom Receiver Configured
- `signoz-otel-collector-config.yaml` shows:
  - `metrics/prometheus` pipeline references `[prometheus]` receiver
  - **BUT**: No prometheus receiver defined in receivers section
  - Only `otlp` receiver exists

#### Finding 3: No /metrics Endpoint
- `curl http://localhost:8080/metrics` → 404
- `curl http://localhost:8080/q/metrics` → 404
- Quarkus not exposing Prometheus metrics endpoint

#### Finding 4: Traces Still Exporting
- Latest span timestamp: 2026-02-26 12:49:09
- 15 → 54 spans after generating traffic
- **OTLP gRPC connection IS working**

## Hypothesis

The metrics in ClickHouse with `prometheusreceiver` scope are:
1. **OLD** - from a previous Prometheus scraping setup
2. **From somewhere else** - not from current k12-backend

## Next Steps

1. ✅ Confirmed OTLP connection works (traces exporting)
2. ⏳ Enable Quarkus metrics endpoint for verification
3. ⏳ Check if Quarkus OpenTelemetry metrics need additional config
4. ⏳ Verify in SigNoz UI directly

## Action Required

Quarkus OpenTelemetry metrics may require:
- Explicit metric exporters configuration
- Or different properties than just `quarkus.otel.metrics.enabled=true`

Need to verify if `quarkus.otel.metrics.enabled=true` is sufficient or if additional configuration is needed.
