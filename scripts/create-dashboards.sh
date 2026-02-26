#!/bin/bash
# Create new dashboards in SigNoz database

# Copy the modified database back
docker cp /tmp/signoz.db k12-signoz:/var/lib/signoz/signoz.db.bak

# Get org_id
ORG_ID=$(sqlite3 /tmp/signoz.db "SELECT id FROM organizations LIMIT 1;")
echo "Using org_id: $ORG_ID"

# Create dashboards with working queries
sqlite3 /tmp/signoz.db <<EOF
-- Dashboard 1: Application Overview
INSERT INTO dashboard (id, created_at, updated_at, created_by, updated_by, data, org_id)
VALUES (
  'k12-app-overview-001',
  datetime('now'),
  datetime('now'),
  'system',
  'system',
  '{"title":"k12-backend - Application Overview","description":"HTTP requests, JVM memory, and GC metrics","version":"1.0","layout":[{"i":"panel-1","x":0,"y":0,"w":12,"h":6},{"i":"panel-2","x":12,"y":0,"w":12,"h":6},{"i":"panel-3","x":0,"y":6,"w":8,"h":6},{"i":"panel-4","x":8,"y":6,"w":8,"h":6},{"i":"panel-5","x":16,"y":6,"w":8,"h":6}],"panels":[]}',
  '$ORG_ID'
);

-- Dashboard 2: Database Connection Pool
INSERT INTO dashboard (id, created_at, updated_at, created_by, updated_by, data, org_id)
VALUES (
  'k12-db-pool-002',
  datetime('now'),
  datetime('now'),
  'system',
  'system',
  '{"title":"k12-backend - Database Pool","description":"Agroal connection pool metrics","version":"1.0","layout":[{"i":"panel-1","x":0,"y":0,"w":12,"h":6},{"i":"panel-2","x":12,"y":0,"w":12,"h":6},{"i":"panel-3","x":0,"y":6,"w":8,"h":6},{"i":"panel-4","x":8,"y":6,"w":8,"h":6},{"i":"panel-5","x":16,"y":6,"w":8,"h":6}],"panels":[]}',
  '$ORG_ID'
);

-- Dashboard 3: JVM Runtime
INSERT INTO dashboard (id, created_at, updated_at, created_by, updated_by, data, org_id)
VALUES (
  'k12-jvm-runtime-003',
  datetime('now'),
  datetime('now'),
  'system',
  'system',
  '{"title":"k12-backend - JVM Runtime","description":"JVM memory, threads, GC, and CPU metrics","version":"1.0","layout":[{"i":"panel-1","x":0,"y":0,"w":12,"h":6},{"i":"panel-2","x":12,"y":0,"w":12,"h":6},{"i":"panel-3","x":0,"y":6,"w":8,"h":6},{"i":"panel-4","x":8,"y":6,"w":8,"h":6},{"i":"panel-5","x":16,"y":6,"w":8,"h":6}],"panels":[]}',
  '$ORG_ID'
);

SELECT 'Dashboards created:' as status;
SELECT id, substr(data, 1, 80) as data_preview FROM dashboard;
EOF

# Copy back to container and restart SigNoz
docker cp /tmp/signoz.db k12-signoz:/var/lib/signoz/signoz.db
docker restart k12-signoz

echo "✓ Dashboards created and SigNoz restarted"
echo "✓ Access dashboards at: http://localhost:3301/dashboards"
