# Test Flow Panel - User Guide

## Overview

The Test Flow Panel allows you to create automated test sequences by connecting API endpoints. You can chain multiple requests together, extract data from responses, and use that data in subsequent requests.

## Features

- **Visual Flow Editor**: Drag and drop endpoints to create test sequences
- **Variable Extraction**: Extract values from responses using JSONPath
- **Variable Substitution**: Use extracted values in subsequent requests
- **Flow Execution**: Run entire sequences or individual nodes
- **Save/Load**: Flows are auto-saved to browser storage
- **Import/Export**: Share flows as JSON files

## Quick Start

### 1. Add Endpoints to Your Flow

1. Click the **"Test Flow"** tab to open the Test Flow Panel
2. Browse available endpoints in the left sidebar (grouped by tag)
3. **Drag** an endpoint from the sidebar and **drop** it onto the canvas

### 2. Connect Endpoints

1. Nodes have connection points on left (input) and right (output)
2. **Drag** from the output connector (right side) of one node
3. **Drop** onto the input connector (left side) of another node
4. An arrow will appear showing the flow direction

### 3. Configure Nodes

1. **Click** on any node to open its configuration panel
2. Configure the following:
   - **Headers**: Add custom headers (e.g., Authorization)
   - **Body**: Set request body (JSON format)
   - **Extract**: Define variables to extract from the response

### 4. Extract Variables

Use JSONPath to extract values from responses:

- `$.token` - Extract top-level `token` field
- `$.user.id` - Extract nested `user.id` field
- `$.data[0].id` - Extract first item's `id` from array

Example:
```json
Response: { "token": "abc123", "user": { "id": 42 } }

Extraction:
- Field: $.token
- As: authToken

Result: {{authToken}} = "abc123"
```

### 5. Use Variables in Requests

Reference extracted variables using double curly braces:

- Headers: `Authorization: Bearer {{authToken}}`
- Body: `{"userId": "{{userId}}"}`
- URL parameters: `?user={{userId}}`

## Example: Authentication Flow

### Scenario: Login → Get User Data

#### Step 1: Login Node

**Endpoint**: `POST /api/auth/login`

**Body**:
```json
{
  "username": "admin",
  "password": "password123"
}
```

**Extract**:
- Field: `$.token`
- As: `authToken`

#### Step 2: Get User Node

**Endpoint**: `GET /api/users/me`

**Headers**:
```
Authorization: Bearer {{authToken}}
```

**Connection**: Connect Login output → Get User input

#### Step 3: Execute Flow

1. Click **"▶ Execute Flow"** or press **Ctrl+Enter**
2. Watch nodes execute in sequence
3. Check status indicators:
   - 🟡 Ready (pending)
   - 🔵 Running
   - ✅ Success (shows status code and time)
   - ❌ Error (shows error message)

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Enter` | Execute entire flow |
| `Ctrl+S` | Save flow |
| `Delete` | Clear all nodes (when no node selected) |
| `Escape` | Deselect node / Close config panel |
| `F1` | Open help |

## Buttons

| Button | Action |
|--------|--------|
| ▶ Execute Flow | Run all connected nodes in sequence |
| Save | Save flow to browser storage |
| Import | Load flow from JSON file |
| Export | Download flow as JSON file |
| Clear | Remove all nodes (requires confirmation) |
| ? | Show help |

## Node Actions

Each node has three buttons:

- **▶** Execute this individual node
- **⚙** Configure node (headers, body, extraction)
- **×** Delete node

## Best Practices

### 1. Variable Naming

Use descriptive names for extracted variables:
- ✅ `authToken`, `userId`, `tenantId`
- ❌ `var1`, `data`, `result`

### 2. Error Handling

If a node fails:
- The error message appears in the node
- The flow stops at that point
- Fix the issue and re-run
- Previous successful nodes retain their results

### 3. Flow Design

- Keep flows focused on a single test scenario
- Use clear, sequential connections
- Test each node individually before running the full flow
- Save your flow frequently (Ctrl+S)

### 4. Debugging

1. Test nodes individually using the ▶ button
2. Check the Variables panel to see extracted values
3. Verify JSONPath expressions are correct
4. Check network requests in browser DevTools

## Common Patterns

### Pattern 1: Authentication Chain

```
Login → Extract Token → Get Tenant (with token) → Create Resource (with token)
```

### Pattern 2: CRUD Operations

```
Create → Extract ID → Read (by ID) → Update → Delete
```

### Pattern 3: Multi-Step Setup

```
Create Tenant → Extract Tenant ID → Create Admin → Create Users → Verify
```

## Tips

- **Drag nodes** to rearrange your flow
- **Click connections** to select them (future: delete connections)
- **Use the search box** to filter endpoints by method or path
- **Export your flows** before major changes for backup
- **Share flows** with team members via export/import

## Troubleshooting

### Variables Not Working

1. Check JSONPath syntax (must start with `$.`)
2. Verify the response contains the expected data
3. Check the Variables panel to see what was extracted
4. Ensure variable name matches exactly (case-sensitive)

### Flow Not Executing in Order

1. Verify connections are in place (output → input)
2. Check for circular dependencies
3. Use the Execute button on individual nodes to test

### Import Fails

1. Verify JSON file is valid
2. Check file was exported from this tool
3. Ensure no manual edits broke the structure
4. Check browser console for specific errors

## Technical Details

### Flow Structure

```json
{
  "nodes": [
    {
      "id": "node-1",
      "method": "POST",
      "path": "/api/auth/login",
      "x": 100,
      "y": 100,
      "config": {
        "headers": {},
        "body": "{\"username\":\"admin\"}",
        "extract": [{"field": "$.token", "as": "authToken"}]
      },
      "status": "success",
      "result": {"status": 200, "time": 123}
    }
  ],
  "connections": [
    {"from": "node-1", "to": "node-2"}
  ],
  "variables": {
    "authToken": {
      "value": "abc123",
      "source": "node-1"
    }
  }
}
```

### Storage

- Flows are saved to `localStorage` under key `test-flow`
- Maximum storage: ~5MB (browser dependent)
- Export for long-term backup

---

**Version**: 1.0
**Last Updated**: 2025-02-28
