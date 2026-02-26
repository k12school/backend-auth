-- Create new dashboards for k12-backend with working queries
-- This file will be loaded into SigNoz SQLite database

INSERT INTO dashboard (id, created_at, updated_at, created_by, updated_by, data, org_id)
VALUES
(
  'k12-app-overview-001',
  datetime('now'),
  datetime('now'),
  'system',
  'system',
  json_object(
    'title', 'k12-backend - Application Overview',
    'description', 'Application metrics: HTTP requests, JVM memory, GC pauses',
    'tags', json_array('k12-backend', 'application', 'jvm', 'http'),
    'layout', json_array(
      json_object('i', 'panel-1', 'x', 0, 'y', 0, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-2', 'x', 12, 'y', 0, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-3', 'x', 0, 'y', 6, 'w', 8, 'h', 6, 'minH', 4),
      json_object('i', 'panel-4', 'x', 8, 'y', 6, 'w', 8, 'h', 6, 'minH', 4),
      json_object('i', 'panel-5', 'x', 16, 'y', 6, 'w', 8, 'h', 6, 'minH', 4)
    ),
    'panels', json_array(
      json_object(
        'id', 'panel-1',
        'title', 'HTTP Request Rate',
        'description', 'Requests per second',
        'queryType', 'metrics',
        'unit', 'requests/sec',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as rate FROM signoz_metrics.samples_v4 WHERE metric_name = ''http.server.requests.count'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Request Rate'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-2',
        'title', 'HTTP Request Duration',
        'description', 'Average request duration',
        'queryType', 'metrics',
        'unit', 'ms',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as duration FROM signoz_metrics.samples_v4 WHERE metric_name = ''http.server.requests.avg'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Avg Duration'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-3',
        'title', 'JVM Heap Memory',
        'description', 'Heap memory usage by area',
        'queryType', 'metrics',
        'unit', 'bytes',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, toString(JSONExtractString(labels, ''area'')) as area, avg(value) as bytes FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.memory.used'' AND JSONExtractString(labels, ''area'') LIKE ''%heap%'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time, area ORDER BY time DESC',
          'legend', 'Heap Usage'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-4',
        'title', 'JVM GC Pauses',
        'description', 'Garbage collection pause time',
        'queryType', 'metrics',
        'unit', 'ms',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as gc_time FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.gc.pause.sum'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'GC Time'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-5',
        'title', 'Active Connections',
        'description', 'Current HTTP connections',
        'queryType', 'metrics',
        'unit', 'connections',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as connections FROM signoz_metrics.samples_v4 WHERE metric_name = ''http.server.active.connections'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Active Connections'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      )
    )
  ),
  (SELECT id FROM organizations LIMIT 1)
),
(
  'k12-db-pool-002',
  datetime('now'),
  datetime('now'),
  'system',
  'system',
  json_object(
    'title', 'k12-backend - Database Connection Pool',
    'description', 'Agroal connection pool metrics',
    'tags', json_array('k12-backend', 'database', 'connection-pool'),
    'layout', json_array(
      json_object('i', 'panel-1', 'x', 0, 'y', 0, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-2', 'x', 12, 'y', 0, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-3', 'x', 0, 'y', 6, 'w', 8, 'h', 6, 'minH', 4),
      json_object('i', 'panel-4', 'x', 8, 'y', 6, 'w', 8, 'h', 6, 'minH', 4),
      json_object('i', 'panel-5', 'x', 16, 'y', 6, 'w', 8, 'h', 6, 'minH', 4),
      json_object('i', 'panel-6', 'x', 0, 'y', 12, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-7', 'x', 12, 'y', 12, 'w', 12, 'h', 6, 'minH', 4)
    ),
    'panels', json_array(
      json_object(
        'id', 'panel-1',
        'title', 'Active Connections',
        'description', 'Currently active database connections',
        'queryType', 'metrics',
        'unit', 'connections',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as active FROM signoz_metrics.samples_v4 WHERE metric_name = ''agroal.active.count'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Active'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-2',
        'title', 'Available Connections',
        'description', 'Available connections in pool',
        'queryType', 'metrics',
        'unit', 'connections',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as available FROM signoz_metrics.samples_v4 WHERE metric_name = ''agroal.available.count'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Available'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-3',
        'title', 'Awaiting Connections',
        'description', 'Threads waiting for connection',
        'queryType', 'metrics',
        'unit', 'threads',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as awaiting FROM signoz_metrics.samples_v4 WHERE metric_name = ''agroal.awaiting.count'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Awaiting'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-4',
        'title', 'Max Used Connections',
        'description', 'Peak connection usage',
        'queryType', 'metrics',
        'unit', 'connections',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, max(value) as max_used FROM signoz_metrics.samples_v4 WHERE metric_name = ''agroal.max.used.count'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Max Used'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-5',
        'title', 'Connection Acquisition Rate',
        'description', 'New connections per second',
        'queryType', 'metrics',
        'unit', 'conns/sec',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as acquire_rate FROM signoz_metrics.samples_v4 WHERE metric_name = ''agroal.acquire.count'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Acquire Rate'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-6',
        'title', 'Average Blocking Time',
        'description', 'Time threads blocked waiting for connection',
        'queryType', 'metrics',
        'unit', 'ms',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as blocking_time FROM signoz_metrics.samples_v4 WHERE metric_name = ''agroal.blocking.time.avg'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Avg Blocking'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-7',
        'title', 'Connection Creation Time',
        'description', 'Time to create new connections',
        'queryType', 'metrics',
        'unit', 'ms',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as creation_time FROM signoz_metrics.samples_v4 WHERE metric_name = ''agroal.creation.time.avg'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Creation Time'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      )
    )
  ),
  (SELECT id FROM organizations LIMIT 1)
),
(
  'k12-jvm-003',
  datetime('now'),
  datetime('now'),
  'system',
  'system',
  json_object(
    'title', 'k12-backend - JVM Runtime',
    'description', 'JVM runtime metrics: memory, threads, GC, CPU',
    'tags', json_array('k12-backend', 'jvm', 'runtime'),
    'layout', json_array(
      json_object('i', 'panel-1', 'x', 0, 'y', 0, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-2', 'x', 12, 'y', 0, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-3', 'x', 0, 'y', 6, 'w', 8, 'h', 6, 'minH', 4),
      json_object('i', 'panel-4', 'x', 8, 'y', 6, 'w', 8, 'h', 6, 'minH', 4),
      json_object('i', 'panel-5', 'x', 16, 'y', 6, 'w', 8, 'h', 6, 'minH', 4),
      json_object('i', 'panel-6', 'x', 0, 'y', 12, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-7', 'x', 12, 'y', 12, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-8', 'x', 0, 'y', 18, 'w', 12, 'h', 6, 'minH', 4),
      json_object('i', 'panel-9', 'x', 12, 'y', 18, 'w', 12, 'h', 6, 'minH', 4)
    ),
    'panels', json_array(
      json_object(
        'id', 'panel-1',
        'title', 'Heap Memory Usage',
        'description', 'JVM heap memory by area',
        'queryType', 'metrics',
        'unit', 'bytes',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, toString(JSONExtractString(labels, ''area'')) as area, avg(value) as bytes FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.memory.used'' AND JSONExtractString(labels, ''area'') LIKE ''%heap%'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time, area ORDER BY time DESC',
          'legend', 'Heap Memory'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-2',
        'title', 'Non-Heap Memory',
        'description', 'JVM non-heap memory (code, metaspace, etc)',
        'queryType', 'metrics',
        'unit', 'bytes',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, toString(JSONExtractString(labels, ''area'')) as area, avg(value) as bytes FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.memory.used'' AND JSONExtractString(labels, ''area'') NOT LIKE ''%heap%'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time, area ORDER BY time DESC',
          'legend', 'Non-Heap Memory'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-3',
        'title', 'Live Thread Count',
        'description', 'Current number of live threads',
        'queryType', 'metrics',
        'unit', 'threads',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as threads FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.threads.live'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Live Threads'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-4',
        'title', 'Peak Thread Count',
        'description', 'Peak number of threads',
        'queryType', 'metrics',
        'unit', 'threads',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, max(value) as peak_threads FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.threads.peak'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Peak Threads'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-5',
        'title', 'Daemon Thread Count',
        'description', 'Number of daemon threads',
        'queryType', 'metrics',
        'unit', 'threads',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as daemon FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.threads.daemon'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Daemon Threads'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-6',
        'title', 'GC Pause Count',
        'description', 'Number of GC pauses',
        'queryType', 'metrics',
        'unit', 'pauses',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as count FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.gc.pause.count'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'GC Count'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-7',
        'title', 'GC Pause Time',
        'description', 'Total time spent in GC',
        'queryType', 'metrics',
        'unit', 'ms',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, sum(value) as gc_time FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.gc.pause.sum'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'GC Time'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-8',
        'title', 'CPU Recent Utilization',
        'description', 'Recent CPU usage',
        'queryType', 'metrics',
        'unit', '%',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) * 100 as cpu_percent FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.cpu.recent_utilization'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'CPU %'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      ),
      json_object(
        'id', 'panel-9',
        'title', 'Loaded Class Count',
        'description', 'Number of classes currently loaded',
        'queryType', 'metrics',
        'unit', 'classes',
        'queryData', json_array(json_object(
          'query', 'SELECT toDateTime64(unix_milli/1000, 3) as time, avg(value) as classes FROM signoz_metrics.samples_v4 WHERE metric_name = ''jvm.classes.loaded'' AND unix_milli > now() - INTERVAL 15 MINUTE GROUP BY time ORDER BY time DESC',
          'legend', 'Loaded Classes'
        )),
        'panelType', 'time',
        'time', json_object('start', 'now-15m', 'end', 'now')
      )
    )
  ),
  (SELECT id FROM organizations LIMIT 1)
);
