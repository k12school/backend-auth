# ✅ Uptime Kuma - FINAL CONFIGURATION

## Your Environment

**Monitoring Network Gateway:** `10.89.9.1`
**Uptime Kuma:** Now configured to use `host.docker.internal` → `10.89.9.1`

**Configuration has been fixed!** `host.docker.internal` will now work correctly.

---

## Setup Instructions

### 1. Open Uptime Kuma
```
http://localhost:3001
```

### 2. Create Monitors

**You can now use `host.docker.internal` in your monitors!**

#### Monitor 1: K12 Backend Health

| Field | Value |
|-------|-------|
| **Display Name** | `K12 Backend - Health` |
| **Type** | `HTTP(s)` |
| **URL** | `http://host.docker.internal:8080/q/health` |
| **Interval** | `1 minute` |
| **Keyword** | `UP` |
| **Response Time Threshold** | `2000 ms` |

#### Monitor 2: K12 Backend Metrics

| Field | Value |
|-------|-------|
| **Display Name** | `K12 Backend - Metrics` |
| **Type** | `HTTP(s)` |
| **URL** | `http://host.docker.internal:8080/metrics` |
| **Interval** | `2 minutes` |

#### Monitor 3: PostgreSQL

| Field | Value |
|-------|-------|
| **Display Name** | `K12 PostgreSQL` |
| **Type** | `TCP Port` |
| **Hostname** | `host.docker.internal` |
| **Port** | `15432` |
| **Interval** | `1 minute` |

#### Monitor 4: Grafana (Optional)

| Field | Value |
|-------|-------|
| **Display Name** | `Grafana` |
| **Type** | `HTTP(s)` |
| **URL** | `http://host.docker.internal:3000` |
| **Interval** | `5 minutes` |

---

## What Was Fixed

**Before:** `host.docker.internal` → `192.168.15.8` (WiFi IP) ❌
**After:** `host.docker.internal` → `10.89.9.1` (Monitoring gateway) ✅

The docker-compose has been updated with:
```yaml
uptime-kuma:
  extra_hosts:
    - "host.docker.internal:10.89.9.1"
```

---

## Verification

You can verify in Uptime Kuma:
1. Create a test monitor with URL: `http://host.docker.internal:8080/q/health`
2. It should show as "UP" with green status

---

## Troubleshooting

If it still shows "DOWN":

1. **Check Quarkus is running:**
   ```bash
   curl http://localhost:8080/q/health
   ```

2. **Check the gateway IP:**
   ```bash
   docker network inspect k12-monitoring | jq -r '.[0].IPAM.Config[0].Gateway'
   # Should return: 10.89.9.1
   ```

3. **Verify Uptime Kuma config:**
   ```bash
   docker exec k12-uptime-kuma cat /etc/hosts | grep host.docker.internal
   # Should show: 10.89.9.1    host.docker.internal
   ```

4. **Restart Uptime Kuma:**
   ```bash
   docker compose -f docker-compose.monitoring.yml restart uptime-kuma
   ```

---

## Summary

✅ **Fixed:** Uptime Kuma can now reach your host machine
✅ **Use:** `host.docker.internal` in all monitors
✅ **Access:** http://localhost:3001

The issue was that `host-gateway` was resolving to the wrong IP. It's now explicitly set to the correct monitoring network gateway (10.89.9.1).
