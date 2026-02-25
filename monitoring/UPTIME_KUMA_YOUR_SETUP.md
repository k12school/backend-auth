# Uptime Kuma Setup - CORRECTED

## Your Environment

**Docker Bridge Gateway:** `10.88.0.1` ✅
**Quarkus:** Listening on `*:8080` ✅

**Important:** Always use `10.88.0.1` when creating monitors in Uptime Kuma

---

## Quick Setup

### 1. Access Uptime Kuma
```
http://localhost:3001
```

### 2. Create Monitors

Use **`10.88.0.1`** as the hostname/IP for all monitors:

**Example:**
- URL: `http://10.88.0.1:8080/q/health`
- Hostname: `10.88.0.1`
- Port: `15432` (for PostgreSQL)

### 3. Recommended Monitors

| Monitor | Type | URL/Hostname | Port | Interval |
|---------|------|--------------|------|----------|
| K12 Backend Health | HTTP | http://10.88.0.1:8080/q/health | - | 1 min |
| K12 Backend Metrics | HTTP | http://10.88.0.1:8080/metrics | - | 2 min |
| PostgreSQL | TCP | 10.88.0.1 | 15432 | 1 min |
| Grafana | HTTP | http://10.88.0.1:3000 | - | 5 min |
| Prometheus | HTTP | http://10.88.0.1:9090 | - | 5 min |

---

## Why NOT host.docker.internal?

On your Linux system:
- `host.docker.internal` → `192.168.15.8` (WiFi IP) ❌
- Docker bridge gateway → `10.88.0.1` ✅

The WiFi IP is not accessible from within Docker containers.

---

## Verification

Test connectivity from Uptime Kuma:
```bash
docker exec k12-uptime-kuma curl http://10.88.0.1:8080/q/health
```

Expected output:
```json
{"status":"UP","checks":[...]}
```

---

## Troubleshooting

### If 10.88.0.1 stops working:

Find your current Docker bridge gateway:
```bash
docker network inspect bridge | jq -r '.[0].IPAM.Config[0].Gateway'
```

Use whatever IP that returns.

### If connection refused:

Check Quarkus is running:
```bash
curl http://localhost:8080/q/health
```

Check Quarkus binding:
```bash
ss -tlnp | grep :8080
# Should show: *:8080 (all interfaces)
```

---

## Summary

✅ **Use:** `10.88.0.1`
❌ **Don't use:** `host.docker.internal` or `192.168.15.8`

This is specific to your Linux Docker setup.
