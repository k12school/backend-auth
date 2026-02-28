# Test Flow Panel - Test Plan

## Test Environment

- File: `/home/joao/workspace/k12/back/api-docs-custom.html`
- API Base: `http://localhost:8080`
- Test Date: 2025-02-28

## Test Cases

### Test Case 1: Basic Flow Creation

**Objective**: Create and execute a simple 3-node authentication flow

**Steps**:
1. Open Test Flow Panel
2. Add Login endpoint (POST /api/auth/login)
3. Add Get Tenant endpoint (GET /api/tenants/current)
4. Add Create Admin endpoint (POST /api/admins)
5. Connect nodes: Login → Get Tenant → Create Admin
6. Configure Login node:
   - Body: `{"username":"test@example.com","password":"password123"}`
   - Extract: `$.token` as `authToken`
7. Configure Get Tenant node:
   - Headers: `Authorization: Bearer {{authToken}}`
8. Configure Create Admin node:
   - Headers: `Authorization: Bearer {{authToken}}`
   - Body: `{"email":"admin@test.com","name":"Test Admin"}`
9. Execute flow

**Expected Results**:
- All nodes execute in sequence
- Login node returns token
- Token is extracted and displayed in Variables panel
- Get Tenant uses token successfully
- Create Admin uses token successfully
- Final status: 3 success, 0 errors

**Status**: ⬜ Not Tested

---

### Test Case 2: Variable Extraction

**Objective**: Verify JSONPath extraction from nested responses

**Steps**:
1. Create Login node
2. Extract multiple fields:
   - `$.token` as `authToken`
   - `$.user.id` as `userId`
   - `$.user.email` as `userEmail`
3. Execute node
4. Check Variables panel

**Expected Results**:
- All three variables appear in Variables panel
- Values match response structure
- Source shows node ID

**Status**: ⬜ Not Tested

---

### Test Case 3: Variable Substitution

**Objective**: Verify variables work in headers, body, and URL

**Steps**:
1. Create flow with variable extraction
2. Use variables in:
   - Headers: `Authorization: Bearer {{authToken}}`
   - Body: `{"userId":"{{userId}}","email":"{{userEmail}}"}`
   - URL: `/api/users/{{userId}}`
3. Execute flow
4. Verify requests contain substituted values

**Expected Results**:
- Variables replaced in all locations
- API receives correct values
- Requests succeed

**Status**: ⬜ Not Tested

---

### Test Case 4: Error Handling

**Objective**: Verify flow stops on error and displays message

**Steps**:
1. Create flow: Valid Node → Invalid Node → Valid Node
2. Make Invalid Node fail (invalid credentials, bad URL, etc.)
3. Execute flow
4. Observe behavior

**Expected Results**:
- Valid Node executes successfully
- Invalid Node shows error status
- Error message displayed in node
- Flow stops (third node doesn't execute)
- Notification shows 1 success, 1 error

**Status**: ⬜ Not Tested

---

### Test Case 5: Import/Export

**Objective**: Verify flow can be saved, exported, imported, and loaded

**Steps**:
1. Create a flow with 3+ nodes
2. Click Export button
3. Verify JSON file downloads
4. Clear flow (Clear button)
5. Click Import button
6. Select exported file
7. Verify nodes and connections restored

**Expected Results**:
- Export produces valid JSON
- Import validates structure
- All nodes recreated in same positions
- All connections restored
- Variables preserved

**Status**: ⬜ Not Tested

---

### Test Case 6: Keyboard Shortcuts

**Objective**: Verify all keyboard shortcuts work

**Steps**:
1. Create flow with nodes
2. Test each shortcut:
   - `Ctrl+Enter`: Execute flow
   - `Ctrl+S`: Save flow (check for toast)
   - `Delete`: Clear flow (with no node selected)
   - `Escape`: Deselect node
   - `F1`: Open help modal

**Expected Results**:
- All shortcuts trigger correct actions
- Toast notifications appear for Save
- Confirmation dialog for Clear
- Help modal opens

**Status**: ⬜ Not Tested

---

### Test Case 7: Node Actions

**Objective**: Verify individual node buttons work correctly

**Steps**:
1. Add 3 nodes to canvas
2. Test each button:
   - Click ▶ on node 2 (only node 2 executes)
   - Click ⚙ on node 1 (config panel opens)
   - Click × on node 3 (node deleted)

**Expected Results**:
- Individual execution works
- Config panel shows correct node data
- Delete removes node
- Connections updated/deleted

**Status**: ⬜ Not Tested

---

### Test Case 8: Canvas Interactions

**Objective**: Verify drag-and-drop and panning work

**Steps**:
1. Drag endpoint from library to canvas
2. Drag node to new position
3. Create connection by dragging from output to input
4. Click and drag canvas to pan

**Expected Results**:
- Endpoint drops at cursor position
- Node moves smoothly
- Connection line updates in real-time
- Canvas pans smoothly

**Status**: ⬜ Not Tested

---

### Test Case 9: Responsive Design

**Objective**: Verify UI works on different screen sizes

**Steps**:
1. Test at widths: 1920px, 1366px, 1024px, 768px, 375px
2. Verify layout adapts:
   - Sidebar becomes overlay or stacks
   - Config panel moves to bottom
   - Node width adjusts
   - Buttons wrap or resize

**Expected Results**:
- No horizontal scrolling
- All features accessible
- Canvas remains usable
- No overlapping elements

**Status**: ⬜ Not Tested

---

### Test Case 10: Edge Cases

**Objective**: Test boundary conditions and error states

**Steps**:
1. Test with empty flow (execute, save, export)
2. Test invalid JSON import (wrong structure)
3. Test circular connections (A→B→A)
4. Test disconnected nodes
5. Test very long variable names
6. Test special characters in values
7. Test rapid clicking (execute button spam)

**Expected Results**:
- Appropriate error messages
- Graceful handling
- No crashes or hangs
- Clear user feedback

**Status**: ⬜ Not Tested

---

## Bug Report Template

### Bug #[NUMBER]: [TITLE]

**Severity**: Critical / High / Medium / Low
**Status**: Open / Fixed / Verified

**Description**:
[Brief description of the bug]

**Steps to Reproduce**:
1.
2.
3.

**Expected Behavior**:
[What should happen]

**Actual Behavior**:
[What actually happens]

**Environment**:
- Browser: [Chrome/Firefox/Safari + version]
- Screen Size: [width x height]
- Console Errors: [if any]

**Attachments**:
- Screenshots
- Console logs
- Flow JSON (if applicable)

---

## Test Results Summary

| Test Case | Status | Notes |
|-----------|--------|-------|
| TC1: Basic Flow Creation | ⬜ Not Tested | |
| TC2: Variable Extraction | ⬜ Not Tested | |
| TC3: Variable Substitution | ⬜ Not Tested | |
| TC4: Error Handling | ⬜ Not Tested | |
| TC5: Import/Export | ⬜ Not Tested | |
| TC6: Keyboard Shortcuts | ⬜ Not Tested | |
| TC7: Node Actions | ⬜ Not Tested | |
| TC8: Canvas Interactions | ⬜ Not Tested | |
| TC9: Responsive Design | ⬜ Not Tested | |
| TC10: Edge Cases | ⬜ Not Tested | |

**Overall Status**: ⬜ 0/10 Passed

---

## Sign-off

**Tested By**: __________________
**Date**: __________________
**Build**: __________________
**Result**: ✅ Pass / ❌ Fail

**Comments**:
_________________________________________________
_________________________________________________
_________________________________________________
