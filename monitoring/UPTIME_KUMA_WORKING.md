# ✅ Uptime Kuma - FINAL WORKING CONFIGURATION

## The Fix

Changed Uptime Kuma to use **host networking** instead of bridge networking.

**Before:** Custom network (couldn't reach host)
**After:** Host networking (shares host network stack) ✅

---

## What This Means

Uptime Kuma now runs directly on your host network, so it can access:
- ✅ `localhost:8080`
- ✅ `127.0.0.1:8080`
- ✅ Any service running on your host machine

---

## Setup Instructions

### 1. Refresh Uptime Kuma

Open in browser: http://localhost:3001
- Press **F5** to refresh
- Or close and reopen the tab

### 2. Update Your Monitor

Edit the existing monitor or create a new one:

| Field | Value |
|-------|-------|
| **Monitor Type** | `HTTP(s)` |
| **Friendly Name** | `K12 Backend - Health` |
| **URL** | `http://localhost:8080/q/health` |
| **Interval** | `1 minute` |

**Advanced:**
- **Method:** `GET`
- **Expected Status:** `200`
- **Keyword:** `UP`

### 3. Save

Click **Save** - it should now show as **UP** (green) ✅

---

## Additional Monitors

### PostgreSQL
- **Type:** `TCP Port`
- **Hostname:** `localhost`
- **Port:** `15432`

### Metrics Endpoint
- **Type:** `HTTP(s)`
- **URL:** `http://localhost:8080/metrics`

### Grafana
- **Type:** `HTTP(s)`
- **URL:** `http://localhost:3000`

---

## Why This Works

**network_mode: host** means:
- Uptime Kuma shares the host's network namespace
- `localhost` = host machine (not the container)
- Same as Prometheus (which successfully scrapes your app)

---

## Verification

From within Uptime Kuma container (which is now on host network):
```bash
# The container can now access localhost
curl http://localhost:8080/q/health
# Returns: {"status":"UP",...}
```

---

## Summary

✅ **Configuration:** network_mode: host
✅ **Use in monitors:** `localhost` instead of `host.docker.internal`
✅ **Works now:** Edit your monitor to use `http://localhost:8080/q/health`

Refresh Uptime Kuma and update your monitor to use **localhost**! 🎯
