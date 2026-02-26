# Redoc API Documentation UI Enhancement - Design Document

**Date:** 2025-02-26
**Author:** Claude Code
**Status:** Approved

## Overview

This document outlines the design for enhancing the custom Redoc API documentation UI with advanced "Try It Out" features, improved UX, and deep Redoc integration for a personal development tool.

## Project Context

**Current Implementation:**
- Single HTML file (`api-docs-custom.html`) with embedded CSS/JS (~400 lines)
- Split-panel layout: Redoc docs (left) + Try It Out panel (right, fixed 500px)
- Basic JWT authentication with localStorage persistence
- Simple request builder (GET, POST, PUT, DELETE, PATCH)
- Response display with color-coded status codes
- Nginx proxy handles CORS for backend communication

**Target Use Case:** Personal development tool for testing API endpoints during development

## Design Goals

1. **Developer Productivity:** Fast workflows, keyboard shortcuts, request history
2. **Deep Redoc Integration:** Click endpoints to auto-populate request builder
3. **Comprehensive Features:** History, favorites, auth profiles, batch execution
4. **Maintainability:** Clean modular architecture despite single-file constraint
5. **Incremental Delivery:** Always maintain working state during development

## Architecture

### Modular Class-Based Architecture

```
Application Root
    │
    ├─→ State Manager (reactive state)
    ├─→ Event Bus (pub/sub communication)
    ├─→ Storage Manager (localStorage wrapper)
    │
    ├─→ Auth Manager (JWT, profiles)
    ├─→ Request Manager (HTTP, params, headers)
    ├─→ History Manager (history, favorites)
    ├─→ UI Manager (rendering, interactions)
    ├─→ Redoc Integrator (deep integration)
    └─→ Advanced Features Manager (batch, chaining)
```

### Core Modules

**1. StateManager**
- Centralized application state
- Reactive updates using Proxy
- Emits events on state changes
- Path-based getters/setters: `get('auth.currentProfile')`, `set('request.method', 'POST')`

**2. EventBus**
- Decoupled communication between modules
- Event types: `request:sent`, `auth:changed`, `redoc:endpoint:clicked`
- Subscribe/emit pattern

**3. StorageManager**
- Namespaced localStorage wrapper
- Auto-save on state changes
- Handles: tokens, history, favorites, settings

**4. AuthManager**
- JWT token management
- Multiple profiles (dev/staging/prod)
- Token expiration warnings
- Login/logout functionality

**5. RequestManager**
- HTTP request execution with timing
- Query parameter builder
- Headers editor
- Form-data support
- Response parsing (JSON/XML)

**6. HistoryManager**
- Request history (last 10)
- Favorites/collections
- Export/import JSON
- One-click replay

**7. RedocIntegrator**
- DOM event interception
- OpenAPI spec caching
- Endpoint click detection
- "Try in Panel" button injection
- Auto-populate request builder
- Schema-to-example generation

**8. UIManager**
- Section collapsing/expanding
- Dark/light mode
- Keyboard shortcuts
- Modal management
- Responsive layout control

**9. AdvancedFeaturesManager**
- Batch request execution
- Request chaining
- Environment variables
- WebSocket testing
- GraphQL query builder

**10. CodeGenerator**
- cURL command generation
- Code snippets (JS, Python, Java)
- Test suite generation

### State Structure

```javascript
{
  auth: {
    currentProfile: 'dev',
    profiles: {
      dev: { token, expiresAt, baseUrl },
      staging: { token, expiresAt, baseUrl },
      prod: { token, expiresAt, baseUrl }
    }
  },
  request: {
    method, url, query, headers, body, bodyType
  },
  response: {
    status, statusText, headers, body, time, size, timestamp
  },
  history: [], // max 10 entries
  favorites: [],
  ui: {
    activeSection, collapsedSections, darkMode, panelWidth, responseView
  },
  advanced: {
    batchRequests, chains, environments, activeEnvironment
  }
}
```

## Redoc Integration Strategy

### Deep Integration Approach

**Goal:** Clicking any endpoint in Redoc auto-populates the request builder.

**Implementation:**

1. **DOM Event Delegation**
   - Intercept clicks on Redoc endpoint elements
   - Parse `data-section-id` attribute (e.g., `section/Operation/get-/api/users`)
   - Extract method and path

2. **OpenAPI Spec Introspection**
   - Fetch and cache OpenAPI spec on initialization
   - Look up endpoint details: parameters, request body schema
   - Generate example JSON from schema

3. **"Try in Panel" Buttons**
   - Inject custom buttons next to each endpoint using MutationObserver
   - Buttons emit events to populate request builder
   - Highlight active endpoint after selection

### Endpoint Detection Flow

```
User clicks endpoint in Redoc
    ↓
RedocIntegrator detects click (DOM event delegation)
    ↓
Extract method & path from data-section-id
    ↓
Lookup endpoint details in OpenAPI spec
    ↓
Emit 'redoc:endpoint:clicked' event
    ↓
RequestManager populates form (method, URL, params, body example)
    ↓
StateManager updates
    ↓
UIManager renders form fields
    ↓
Highlight endpoint in Redoc
```

## UI/UX Design

### Layout

```
┌─────────────────────────────────────────┐
│ Header: Logo | Dark Mode | Settings    │
├──────────────┬──────────────────────────┤
│              │ ┌──────────────────────┐ │
│   Redoc      │ │ Auth Profile         │ │
│   Docs       │ ├──────────────────────┤ │
│              │ │ Quick Actions        │ │
│  (Scrollable)│ ├──────────────────────┤ │
│              │ │ Request Builder      │ │
│              │ │ [Method] [URL]       │ │
│             ───┤ │ Query Params ▼      │ │
│              │ │ Headers ▼            │ │
│              │ │ Body ▼               │ │
│              │ ├──────────────────────┤ │
│              │ │ Response             │ │
│              │ │ [Status] [Time]      │ │
│              │ │ [Copy] [Download]    │ │
│              │ ├──────────────────────┤ │
│              │ │ History ▼            │ │
│              │ │ Favorites ▼          │ │
│              │ └──────────────────────┘ │
└──────────────┴──────────────────────────┘
```

### Responsive Breakpoints

- **Mobile (< 768px):** Single column, stacked, overlay panel
- **Tablet (768-1024px):** Side-by-side, 400px panel, collapsible
- **Desktop (1024px+):** Full side-by-side, 500px panel (resizable)

### Key Components

**1. Collapsible Sections**
- Each major section can be collapsed
- Chevron indicator (▶ / ▼)
- State persisted in localStorage

**2. Dark/Light Mode**
- CSS custom properties for theming
- Toggle in header
- Preference saved to localStorage

**3. Auth Profile Selector**
- Dropdown in header
- Switch between dev/staging/prod
- Each profile has independent token and base URL
- Token expiration warnings

**4. Keyboard Shortcuts**
- `Ctrl+Enter`: Send request
- `Ctrl+H`: Toggle history
- `Ctrl+F`: Toggle favorites
- `Ctrl+K`: Focus URL input
- `Ctrl+D`: Toggle dark mode
- `Ctrl+N`: New request
- `Escape`: Close modal

**5. Query Parameters Builder**
- Dynamic key-value pairs
- Auto-generated URL
- Add/remove rows

**6. Headers Editor**
- Pre-filled common headers
- Custom header support
- Key-value pairs

**7. Request History**
- Last 10 requests
- One-click replay
- Shows method, URL, status, time
- Clear all button

**8. Response Display**
- Syntax highlighted JSON
- Response time (ms)
- Response size (bytes)
- Pretty/Raw/Headers view toggle
- Copy/Download buttons

### Error Handling

**Error Classification:**
- **Validation:** Invalid URL, missing fields (low severity, recoverable)
- **Network:** CORS, timeout, connection refused (medium severity, retryable)
- **Auth:** Token expired, unauthorized (high severity, requires action)
- **Server:** 5xx errors (high severity, not recoverable)

**Error Display:**
```
┌─────────────────────────────────────┐
│ ❌ [Error Title]                    │
│                                     │
│ [Error message]                     │
│                                     │
│ 💡 Suggestions:                     │
│ • Actionable suggestion 1           │
│ • Actionable suggestion 2           │
│                                     │
│ [Retry] [Copy Error] [Dismiss]      │
└─────────────────────────────────────┘
```

**Recovery Actions:**
- CORS error → Check nginx config
- Token expired → Login again
- Connection refused → Check if backend running
- Timeout → Retry request

## Implementation Phases

### Phase 1: Foundation & Core Refactoring
- Set up module architecture (StateManager, EventBus, StorageManager)
- Refactor existing code into modules
- Dark/light mode toggle
- Collapsible sections
- Basic responsive design
- **Estimated: ~600-800 lines**

### Phase 2: Request Management
- Request history (last 10)
- Favorites/collections
- Query parameters builder
- Headers editor
- Export/import collections
- **Estimated: ~400-500 lines**

### Phase 3: Response Enhancements
- JSON syntax highlighting
- Response timing display
- Response size display
- Copy/Download response buttons
- Pretty/Raw/Headers view toggle
- **Estimated: ~300-400 lines**

### Phase 4: Authentication Enhancements
- Multiple auth profiles
- Token expiration warning
- Logout button
- Bearer token auto-detection
- **Estimated: ~200-300 lines**

### Phase 5: Redoc Integration
- DOM event interception
- OpenAPI spec fetching and caching
- Endpoint click detection
- "Try in Panel" button injection
- Auto-populate request builder
- Active endpoint highlighting
- Example body generation from schema
- **Estimated: ~400-500 lines**

### Phase 6: Advanced Request Builder
- Form-data support for file uploads
- Request validation before sending
- Auto-complete for known endpoints
- Request duplication
- **Estimated: ~300-400 lines**

### Phase 7: Advanced Features
- Batch request execution
- Request chaining (use response A in request B)
- Environment variables support
- cURL generation
- Code snippet generation (JS, Python, Java)
- **Estimated: ~500-600 lines**

### Phase 8: Polish & Testing
- Help modal with all features
- Keyboard shortcuts reference
- Example requests
- Browser compatibility fixes
- Performance optimization
- Comprehensive testing
- **Estimated: ~200-300 lines**

### Total Estimated Code Size

- **Current:** ~400 lines
- **After Phase 4:** ~1,700 lines (all Priority 1 features)
- **After Phase 7:** ~2,800 lines (all features)
- **After Phase 8:** ~3,000 lines (complete, tested)

## Technical Requirements

### Constraints
- **Single-file architecture:** All JS/CSS in `api-docs-custom.html`
- **No frameworks:** Vanilla JavaScript only
- **Browser support:** Chrome 90+, Firefox 88+, Safari 14+, Edge 90+
- **CORS:** Nginx proxy must handle cross-origin requests
- **Docker:** Containerization must work correctly

### Testing
- Unit tests for JavaScript modules
- Integration tests with Docker
- Manual testing checklist
- Browser compatibility testing

### Deployment
```bash
# Rebuild Docker image
docker buildx build --load -f redoc-custom.Dockerfile -t k12-redoc:latest .

# Restart container
docker stop k12-redoc && docker rm k12-redoc
docker run -d --name k12-redoc --network k12-monitoring -p 8081:80 k12-redoc:latest
```

## Success Criteria

1. ✅ All new features work without breaking existing functionality
2. ✅ UI remains responsive and fast
3. ✅ Docker image builds and runs successfully
4. ✅ Backend API communication works (no CORS issues)
5. ✅ Code is well-commented and maintainable
6. ✅ Features work across modern browsers
7. ✅ Redoc integration enables seamless workflow

## Next Steps

1. ✅ Design document approved
2. **Next:** Create detailed implementation plan using writing-plans skill
3. Implement Phase 1 (Foundation)
4. Test and deploy
5. Continue through Phase 8

## Appendix: File Structure

```html
<!DOCTYPE html>
<html>
<head>
  <title>K12 API Documentation</title>
  <style>
    /* CSS (~800 lines) */
    /* Variables, Layout, Components, Responsive, Animations */
  </style>
</head>
<body>
  <!-- HTML Structure -->
  <script>
    // JavaScript (~2,200 lines)

    // === UTILITIES ===
    class EventEmitter { }
    class StorageManager { }

    // === STATE MANAGEMENT ===
    class StateManager { }

    // === CORE MODULES ===
    class AuthManager { }
    class RequestManager { }
    class HistoryManager { }
    class UIManager { }

    // === INTEGRATION ===
    class RedocIntegrator { }

    // === ADVANCED FEATURES ===
    class AdvancedFeaturesManager { }
    class CodeGenerator { }

    // === INITIALIZATION ===
    const app = new APIConsole();
    app.init();
  </script>
</body>
</html>
```
