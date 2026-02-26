#!/usr/bin/env python3
import json
import uuid
import sqlite3

# Copy database
import subprocess
subprocess.run(['docker', 'cp', 'k12-signoz:/var/lib/signoz/signoz.db', '/tmp/signoz.db'], check=True)

# Get org_id
conn = sqlite3.connect('/tmp/signoz.db')
cursor = conn.cursor()
cursor.execute("SELECT id FROM organizations LIMIT 1")
org_id = cursor.fetchone()[0]
print(f"Using org_id: {org_id}")

# Dashboard 1: Application Overview
panel_ids_1 = [str(uuid.uuid4()) for _ in range(5)]

dashboard1_data = {
    'title': 'k12-backend - Application Overview',
    'description': 'HTTP requests, JVM memory, and GC metrics',
    'version': '1.0',
    'layout': [
        {'i': panel_ids_1[0], 'x': 0, 'y': 0, 'w': 12, 'h': 6, 'minH': 4},
        {'i': panel_ids_1[1], 'x': 12, 'y': 0, 'w': 12, 'h': 6, 'minH': 4},
        {'i': panel_ids_1[2], 'x': 0, 'y': 6, 'w': 8, 'h': 6, 'minH': 4},
        {'i': panel_ids_1[3], 'x': 8, 'y': 6, 'w': 8, 'h': 6, 'minH': 4},
        {'i': panel_ids_1[4], 'x': 16, 'y': 6, 'w': 8, 'h': 6, 'minH': 4}
    ],
    'panels': [
        {
            'id': panel_ids_1[0],
            'title': 'HTTP Request Rate',
            'description': 'Requests per second',
            'queryType': 'metrics',
            'unit': 'requests/sec',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'http.server.requests.count',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as rate FROM signoz_metrics.samples_v4 WHERE metric_name = 'http.server.requests.count' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Request Rate'
            }]
        },
        {
            'id': panel_ids_1[1],
            'title': 'HTTP Request Duration',
            'description': 'Average request duration',
            'queryType': 'metrics',
            'unit': 'ms',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'http.server.requests.avg',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as duration FROM signoz_metrics.samples_v4 WHERE metric_name = 'http.server.requests.avg' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Avg Duration'
            }]
        },
        {
            'id': panel_ids_1[2],
            'title': 'JVM Heap Memory',
            'description': 'Heap memory usage by area',
            'queryType': 'metrics',
            'unit': 'bytes',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.memory.used',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, toString(JSONExtractString(labels, 'area')) as area, avg(value) as bytes FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.memory.used' AND JSONExtractString(labels, 'area') LIKE '%heap%' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time, area ORDER BY time DESC",
                'legend': 'Heap Usage'
            }]
        },
        {
            'id': panel_ids_1[3],
            'title': 'GC Pauses',
            'description': 'Garbage collection pause time',
            'queryType': 'metrics',
            'unit': 'ms',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.gc.pause.sum',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as gc_time FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.gc.pause.sum' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'GC Time'
            }]
        },
        {
            'id': panel_ids_1[4],
            'title': 'Active Connections',
            'description': 'Current HTTP connections',
            'queryType': 'metrics',
            'unit': 'connections',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'http.server.active.connections',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as connections FROM signoz_metrics.samples_v4 WHERE metric_name = 'http.server.active.connections' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Active Connections'
            }]
        }
    ]
}

# Update dashboard 1
dashboard1_json = json.dumps(dashboard1_data)
cursor.execute("UPDATE dashboard SET data = ? WHERE json_extract(data, '$.title') = 'k12-backend - Application Overview'", (dashboard1_json,))
print("✓ Updated dashboard 1: Application Overview")

# Dashboard 2: Database Pool
panel_ids_2 = [str(uuid.uuid4()) for _ in range(5)]

dashboard2_data = {
    'title': 'k12-backend - Database Pool',
    'description': 'Agroal connection pool metrics',
    'version': '1.0',
    'layout': [
        {'i': panel_ids_2[0], 'x': 0, 'y': 0, 'w': 12, 'h': 6, 'minH': 4},
        {'i': panel_ids_2[1], 'x': 12, 'y': 0, 'w': 12, 'h': 6, 'minH': 4},
        {'i': panel_ids_2[2], 'x': 0, 'y': 6, 'w': 8, 'h': 6, 'minH': 4},
        {'i': panel_ids_2[3], 'x': 8, 'y': 6, 'w': 8, 'h': 6, 'minH': 4},
        {'i': panel_ids_2[4], 'x': 16, 'y': 6, 'w': 8, 'h': 6, 'minH': 4}
    ],
    'panels': [
        {
            'id': panel_ids_2[0],
            'title': 'Active Connections',
            'description': 'Currently active database connections',
            'queryType': 'metrics',
            'unit': 'connections',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'agroal.active.count',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as active FROM signoz_metrics.samples_v4 WHERE metric_name = 'agroal.active.count' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Active'
            }]
        },
        {
            'id': panel_ids_2[1],
            'title': 'Available Connections',
            'description': 'Available connections in pool',
            'queryType': 'metrics',
            'unit': 'connections',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'agroal.available.count',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as available FROM signoz_metrics.samples_v4 WHERE metric_name = 'agroal.available.count' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Available'
            }]
        },
        {
            'id': panel_ids_2[2],
            'title': 'Awaiting Connections',
            'description': 'Threads waiting for connection',
            'queryType': 'metrics',
            'unit': 'threads',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'agroal.awaiting.count',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as awaiting FROM signoz_metrics.samples_v4 WHERE metric_name = 'agroal.awaiting.count' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Awaiting'
            }]
        },
        {
            'id': panel_ids_2[3],
            'title': 'Max Used Connections',
            'description': 'Peak connection usage',
            'queryType': 'metrics',
            'unit': 'connections',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'agroal.max.used.count',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, max(value) as max_used FROM signoz_metrics.samples_v4 WHERE metric_name = 'agroal.max.used.count' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Max Used'
            }]
        },
        {
            'id': panel_ids_2[4],
            'title': 'Connection Acquisition Rate',
            'description': 'New connections per second',
            'queryType': 'metrics',
            'unit': 'conns/sec',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'agroal.acquire.count',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as acquire_rate FROM signoz_metrics.samples_v4 WHERE metric_name = 'agroal.acquire.count' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Acquire Rate'
            }]
        }
    ]
}

dashboard2_json = json.dumps(dashboard2_data)
cursor.execute("UPDATE dashboard SET data = ? WHERE json_extract(data, '$.title') = 'k12-backend - Database Pool'", (dashboard2_json,))
print("✓ Updated dashboard 2: Database Pool")

# Dashboard 3: JVM Runtime
panel_ids_3 = [str(uuid.uuid4()) for _ in range(9)]

dashboard3_data = {
    'title': 'k12-backend - JVM Runtime',
    'description': 'JVM memory, threads, GC, and CPU metrics',
    'version': '1.0',
    'layout': [
        {'i': panel_ids_3[0], 'x': 0, 'y': 0, 'w': 12, 'h': 6, 'minH': 4},
        {'i': panel_ids_3[1], 'x': 12, 'y': 0, 'w': 12, 'h': 6, 'minH': 4},
        {'i': panel_ids_3[2], 'x': 0, 'y': 6, 'w': 8, 'h': 6, 'minH': 4},
        {'i': panel_ids_3[3], 'x': 8, 'y': 6, 'w': 8, 'h': 6, 'minH': 4},
        {'i': panel_ids_3[4], 'x': 16, 'y': 6, 'w': 8, 'h': 6, 'minH': 4},
        {'i': panel_ids_3[5], 'x': 0, 'y': 12, 'w': 12, 'h': 6, 'minH': 4},
        {'i': panel_ids_3[6], 'x': 12, 'y': 12, 'w': 12, 'h': 6, 'minH': 4},
        {'i': panel_ids_3[7], 'x': 0, 'y': 18, 'w': 12, 'h': 6, 'minH': 4},
        {'i': panel_ids_3[8], 'x': 12, 'y': 18, 'w': 12, 'h': 6, 'minH': 4}
    ],
    'panels': [
        {
            'id': panel_ids_3[0],
            'title': 'Heap Memory Usage',
            'description': 'JVM heap memory by area',
            'queryType': 'metrics',
            'unit': 'bytes',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.memory.used',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, toString(JSONExtractString(labels, 'area')) as area, avg(value) as bytes FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.memory.used' AND JSONExtractString(labels, 'area') LIKE '%heap%' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time, area ORDER BY time DESC",
                'legend': 'Heap Memory'
            }]
        },
        {
            'id': panel_ids_3[1],
            'title': 'Non-Heap Memory',
            'description': 'JVM non-heap memory (code, metaspace, etc)',
            'queryType': 'metrics',
            'unit': 'bytes',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.memory.used',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, toString(JSONExtractString(labels, 'area')) as area, avg(value) as bytes FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.memory.used' AND JSONExtractString(labels, 'area') NOT LIKE '%heap%' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time, area ORDER BY time DESC",
                'legend': 'Non-Heap Memory'
            }]
        },
        {
            'id': panel_ids_3[2],
            'title': 'Live Thread Count',
            'description': 'Current number of live threads',
            'queryType': 'metrics',
            'unit': 'threads',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.threads.live',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as threads FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.threads.live' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Live Threads'
            }]
        },
        {
            'id': panel_ids_3[3],
            'title': 'Peak Thread Count',
            'description': 'Peak number of threads',
            'queryType': 'metrics',
            'unit': 'threads',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.threads.peak',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, max(value) as peak_threads FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.threads.peak' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Peak Threads'
            }]
        },
        {
            'id': panel_ids_3[4],
            'title': 'Daemon Thread Count',
            'description': 'Number of daemon threads',
            'queryType': 'metrics',
            'unit': 'threads',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.threads.daemon',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as daemon FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.threads.daemon' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Daemon Threads'
            }]
        },
        {
            'id': panel_ids_3[5],
            'title': 'GC Pause Count',
            'description': 'Number of GC pauses',
            'queryType': 'metrics',
            'unit': 'pauses',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.gc.pause.count',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as count FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.gc.pause.count' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'GC Count'
            }]
        },
        {
            'id': panel_ids_3[6],
            'title': 'GC Pause Time',
            'description': 'Total time spent in GC',
            'queryType': 'metrics',
            'unit': 'ms',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.gc.pause.sum',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as gc_time FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.gc.pause.sum' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'GC Time'
            }]
        },
        {
            'id': panel_ids_3[7],
            'title': 'CPU Recent Utilization',
            'description': 'Recent CPU usage',
            'queryType': 'metrics',
            'unit': '%',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.cpu.recent_utilization',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) * 100 as cpu_percent FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.cpu.recent_utilization' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'CPU %'
            }]
        },
        {
            'id': panel_ids_3[8],
            'title': 'Loaded Class Count',
            'description': 'Number of classes currently loaded',
            'queryType': 'metrics',
            'unit': 'classes',
            'panelType': 'time',
            'time': {'start': 'now-15m', 'end': 'now'},
            'queryData': [{
                'name': 'jvm.classes.loaded',
                'query': "SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as classes FROM signoz_metrics.samples_v4 WHERE metric_name = 'jvm.classes.loaded' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC",
                'legend': 'Loaded Classes'
            }]
        }
    ]
}

dashboard3_json = json.dumps(dashboard3_data)
cursor.execute("UPDATE dashboard SET data = ? WHERE json_extract(data, '$.title') = 'k12-backend - JVM Runtime'", (dashboard3_json,))
print("✓ Updated dashboard 3: JVM Runtime")

conn.commit()

# Copy back and restart
subprocess.run(['docker', 'cp', '/tmp/signoz.db', 'k12-signoz:/var/lib/signoz/signoz.db'], check=True)
subprocess.run(['docker', 'restart', 'k12-signoz'], check=True)

print("\n✓ All dashboards updated with panels")
print("✓ SigNoz restarted")
print("✓ Access at: http://localhost:3301/dashboards")

# Show summary
cursor.execute("SELECT id, json_extract(data, '$.title') as title, json_extract(data, '$.panels') as panels FROM dashboard WHERE json_extract(data, '$.title') LIKE 'k12-backend%'")
results = cursor.fetchall()
for row in results:
    panel_count = len(json.loads(row[2])) if row[2] else 0
    print(f"\n{row[1][:40]}: {panel_count} panels")

conn.close()
