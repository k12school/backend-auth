# Uptime Kuma Setup Guide

## Monitoring Host Machine Services from Docker

Since your Quarkus application runs on the host machine and Uptime Kuma runs in Docker, use `host.docker.internal` to access host services.

---

## First-Time Setup

### 1. Create Admin Account

1. Open http://localhost:3001
2. Click **"Create Administrator"**
3. Set username and password
4. Click **"Create"**

---

## Recommended Monitors

### 1. K12 Backend Health Check

**Basic Settings:**
- **Display Name:** `K12 Backend - Health`
- **Monitor Type:** `HTTP(s)`
- **URL:** `http://host.docker.internal:8080/q/health`
- **Interval:** `1 minute`

**Advanced Settings:**
- **Method:** `GET`
- **Expected Status Code:** `200`
- **Response Time Threshold:** `2000 ms` (alert if slower)

**Optional:**
- **Keyword:** `{"status":"UP"}` (check for this in response body)

---

### 2. K12 Backend Metrics Endpoint

**Basic Settings:**
- **Display Name:** `K12 Backend - Metrics`
- **Monitor Type:** `HTTP(s)`
- **URL:** `http://host.docker.internal:8080/metrics`
- **Interval:** `2 minutes`

**Advanced Settings:**
- **Method:** `GET`
- **Expected Status Code:** `200`
- **Response Time Threshold:** `5000 ms`

---

### 3. PostgreSQL Database

**Basic Settings:**
- **Display Name:** `K12 PostgreSQL`
- **Monitor Type:** `TCP Port`
- **Hostname:** `host.docker.internal`
- **Port:** `15432`
- **Interval:** `1 minute`

**Note:** This checks if the database port is accepting connections

---

### 4. Grafana Dashboard

**Basic Settings:**
- **Display Name:** `Grafana Dashboard`
- **Monitor Type:** `HTTP(s)`
- **URL:** `http://host.docker.internal:3000`
- **Interval:** `5 minutes`

**Advanced Settings:**
- **Method:** `GET`
- **Expected Status Code:** `200`

---

### 5. Prometheus Metrics

**Basic Settings:**
- **Display Name:** `Prometheus Server`
- **Monitor Type:** `HTTP(s)`
- **URL:** `http://host.docker.internal:9090`
- **Interval:** `5 minutes`

**Advanced Settings:**
- **Method:** `GET`
- **Expected Status Code:** `200`

---

## Alternative: Use Docker Bridge Gateway

If `host.docker.internal` doesn't work (sometimes happens on Linux), find your Docker bridge gateway:

```bash
# Find the gateway IP
docker network inspect bridge | jq -r '.[0].IPAM.Config[0].Gateway'
```

Then use that IP instead of `host.docker.internal` (e.g., `172.17.0.1`)

---

## Notifications Setup

### Enable Email Alerts

1. Go to **Settings** → **Notification**
2. Click **"Setup Notification"**
3. Select **"Email"**
4. Configure:
   - **SMTP Host:** `smtp.gmail.com` (or your SMTP server)
   - **Port:** `587`
   - **Username:** `your-email@gmail.com`
   - **Password:** Your app password
   - **From Email:** `your-email@gmail.com`
   - **To Email:** `your-email@gmail.com`
5. Test and save

### Enable Other Notifications

- **Telegram:** Create bot and use token
- **Slack:** Use webhook URL
- **Discord:** Use webhook URL
- **Webhook:** POST to any URL

---

## Status Page

### Create Public Status Page

1. Go to **Status Page**
2. Click **"Add New Status Page"
3. Configure:
   - **Title:** `K12 Backend API Status`
   - **Slug:** `k12-status`
   - **Description:** `Real-time status of K12 Backend services`
4. **Select Monitors:** Add your created monitors
5. **Theme:** Choose your preference
6. **Save**

Your status page will be available at: `http://localhost:3001/status/k12-status`

---

## Advanced: Multi-Region Monitoring

For production, you can deploy additional Uptime Kuma instances in different regions:

1. Deploy Uptime Kuma on a VPS in another region
2. Add it as an additional monitor in your main Uptime Kuma
3. This checks your service from multiple geographic locations

---

## Example Monitor Configurations

### JSON Import (Optional)

Save this as `uptime-kuma-monitors.json` and import:

```json
[
  {
    "name": "K12 Backend - Health",
    "type": "http",
    "url": "http://host.docker.internal:8080/q/health",
    "method": "GET",
    "interval": 60,
    "timeout": 20,
    "expectedStatusCodes": ["200"],
    "keyword": "UP"
  },
  {
    "name": "K12 PostgreSQL",
    "type": "tcp",
    "hostname": "host.docker.internal",
    "port": 15432,
    "interval": 60
  }
]
```

---

## Troubleshooting

### "host.docker.internal" doesn't work

**Solution 1:** Use Docker bridge gateway
```bash
# Get gateway IP
docker network inspect bridge | jq -r '.[0].IPAM.Config[0].Gateway'
# Use that IP in monitors
```

**Solution 2:** Add extra_hosts to docker-compose
```yaml
services:
  uptime-kuma:
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

### Monitor shows down but service is up

**Check:**
1. Can Uptime Kuma container reach the host?
   ```bash
   docker exec k12-uptime-kuma curl http://host.docker.internal:8080/q/health
   ```

2. Is Quarkus bound to 0.0.0.0?
   ```bash
   ss -tlnp | grep :8080
   # Should show: 0.0.0.0:8080 (not 127.0.0.1:8080)
   ```

### Can't receive notifications

**Check:**
- SMTP settings are correct
- Firewall allows outbound SMTP
- Use app password for Gmail (not account password)
- Test notification in Settings → Notification

---

## Quick Setup Commands

```bash
# Test connectivity from Uptime Kuma to host
docker exec k12-uptime-kuma curl -s http://host.docker.internal:8080/q/health

# Get Docker bridge gateway (alternative to host.docker.internal)
docker network inspect bridge | jq -r '.[0].IPAM.Config[0].Gateway'

# View Uptime Kuma logs
docker logs k12-uptime-kuma --tail 50

# Restart Uptime Kuma
docker compose -f docker-compose.monitoring.yml restart uptime-kuma
```

---

## Best Practices

1. **Check Frequency:**
   - Critical services (API, DB): 1 minute
   - Dashboards (Grafana, Prometheus): 5 minutes
   - Less critical: 10-15 minutes

2. **Alert Thresholds:**
   - Response time: 2000-5000 ms depending on endpoint
   - Set escalation if down for > 5 minutes

3. **Notifications:**
   - Don't alert on every blip
   - Use "Resend Notification" every 30 minutes for ongoing issues

4. **Status Page:**
   - Make it public for users
   - Keep it updated
   - Include historical uptime (90 days)

5. **Maintenance:**
   - Use maintenance mode during deployments
   - Don't spam during planned downtime

---

## Summary

**Key Point:** Always use `host.docker.internal` when monitoring host machine services from Docker containers.

**Monitors to Create:**
1. ✅ K12 Backend Health (http://host.docker.internal:8080/q/health)
2. ✅ K12 Backend Metrics (http://host.docker.internal:8080/metrics)
3. ✅ PostgreSQL (host.docker.internal:15432)
4. ✅ Grafana (http://host.docker.internal:3000)
5. ✅ Prometheus (http://host.docker.internal:9090)

**Access:** http://localhost:3001
