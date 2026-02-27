#!/bin/bash

# ============================================
# API Testing Examples for K12 Backend
# ============================================

BASE_URL="http://localhost:8080"

echo "=== K12 Backend API Test Examples ==="
echo ""

# 1. Health Check
echo "1. Health Check:"
curl -s "${BASE_URL}/q/health" | jq .
echo -e "\n"

# 2. Get OpenAPI Spec
echo "2. Get OpenAPI Spec:"
curl -s "${BASE_URL}/q/openapi" | head -20
echo -e "\n"

# 3. Login (Get JWT Token)
echo "3. Login as admin:"
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@k12.com",
    "password": "admin123"
  }')

echo "$LOGIN_RESPONSE" | jq .
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty')

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  echo -e "\n✓ Login successful! Token: ${TOKEN:0:20}..."
  echo ""

  # 4. Create Tenant Admin (example - requires valid token)
  echo "4. Create Tenant Admin:"
  curl -s -X POST "${BASE_URL}/api/tenants/1/admins" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{
      "email": "tenant-admin@example.com",
      "password": "SecurePass123!",
      "name": "Tenant Admin User",
      "permissions": ["MANAGE_USERS", "VIEW_REPORTS"]
    }' | jq .
else
  echo -e "\n✗ Login failed - check credentials"
fi

echo ""
echo "=== Test Complete ==="
