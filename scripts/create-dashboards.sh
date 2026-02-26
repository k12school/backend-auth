#!/bin/bash
# Create new dashboards in SigNoz database with valid UUIDs

# Generate proper UUIDs
UUID1=$(python3 -c "import uuid; print(str(uuid.uuid4()))")
UUID2=$(python3 -c "import uuid; print(str(uuid.uuid4()))")
UUID3=$(python3 -c "import uuid; print(str(uuid.uuid4()))")

echo "Creating dashboards with IDs:"
echo "1: $UUID1"
echo "2: $UUID2"
echo "3: $UUID3"

# Copy database locally to work with it
docker cp k12-signoz:/var/lib/signoz/signoz.db /tmp/signoz.db.tmp
mv /tmp/signoz.db.tmp /tmp/signoz.db

# Get org_id
ORG_ID=$(sqlite3 /tmp/signoz.db "SELECT id FROM organizations LIMIT 1;")
echo "Using org_id: $ORG_ID"

# Delete existing dashboards with our names if they exist
sqlite3 /tmp/signoz.db "DELETE FROM dashboard WHERE json_extract(data, '$.title') LIKE 'k12-backend%';"

# Create dashboards with proper UUIDs
sqlite3 /tmp/signoz.db <<EOF
-- Dashboard 1: Application Overview
INSERT INTO dashboard (id, created_at, updated_at, created_by, updated_by, data, org_id)
VALUES (
  '$UUID1',
  datetime('now'),
  datetime('now'),
  'system',
  'system',
  '{"title":"k12-backend - Application Overview","description":"HTTP requests, JVM memory, and GC metrics","version":"1.0","layout":[],"panels":[]}',
  '$ORG_ID'
);

-- Dashboard 2: Database Connection Pool
INSERT INTO dashboard (id, created_at, updated_at, created_by, updated_by, data, org_id)
VALUES (
  '$UUID2',
  datetime('now'),
  datetime('now'),
  'system',
  'system',
  '{"title":"k12-backend - Database Pool","description":"Agroal connection pool metrics","version":"1.0","layout":[],"panels":[]}',
  '$ORG_ID'
);

-- Dashboard 3: JVM Runtime
INSERT INTO dashboard (id, created_at, updated_at, created_by, updated_by, data, org_id)
VALUES (
  '$UUID3',
  datetime('now'),
  datetime('now'),
  'system',
  'system',
  '{"title":"k12-backend - JVM Runtime","description":"JVM memory, threads, GC, and CPU metrics","version":"1.0","layout":[],"panels":[]}',
  '$ORG_ID'
);

SELECT 'Dashboards created:' as status;
SELECT substr(id, 1, 36) as id, json_extract(data, '$.title') as title FROM dashboard;
EOF

# Copy back to container and restart SigNoz
docker cp /tmp/signoz.db k12-signoz:/var/lib/signoz/signoz.db
docker restart k12-signoz

echo ""
echo "✓ Dashboards created and SigNoz restarted"
echo "✓ Access dashboards at: http://localhost:3301/dashboards"
echo ""
echo "Next: Add panels using queries from DASHBOARD_SETUP_GUIDE.md"
