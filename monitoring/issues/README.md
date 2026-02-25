# Telemetry Issues - Investigation Files

**Generated:** 2026-02-25 15:25:39 -03:00  
**Status:** Investigation Complete - Ready for Root Cause Analysis

---

## Quick Reference

### Current Status
**Overall Observability:** ❌ **DOWN** (0% data collection)

### Critical Issues
1. **Trace Export Failures** - 100% failure rate
2. **Metrics Scrape Stopped** - 0 collection (59 minutes)
3. **Log Export Blocked** - Protocol mismatch

---

## Document Index

### 1. [Issues List](./ISSUES_LIST.md)
**Purpose:** Complete catalog of all identified issues

**Contains:**
- 4 categorized issues with classifications
- Exact error messages and symptoms
- Timestamps and frequency analysis
- Configuration details for each issue
- Failure classification summary

**Use when:** You need to understand what's broken

---

### 2. [Reproduction Evidence](./REPRODUCTION_EVIDENCE.md)
**Purpose:** Raw evidence collected during issue reproduction

**Contains:**
- Step-by-step reproduction procedures
- Raw log outputs
- HTTP responses
- Error message captures
- Timestamp correlations
- Configuration analysis

**Use when:** You need to reproduce issues or verify findings

---

### 3. [Signal Correlation](./SIGNAL_CORRELATION.md)
**Purpose:** End-to-end signal flow analysis

**Contains:**
- Signal flow diagrams
- Correlation matrix (production → UI)
- Detailed breakdown by layer
- Cross-cutting issues analysis
- Timing correlations

**Use when:** You need to understand how signals flow through the system

---

### 4. [Impact Assessment](./IMPACT_ASSESSMENT.md)
**Purpose:** Business and operational impact analysis

**Contains:**
- Data loss quantification
- Observability coverage gaps
- Comparison: Dev vs Docker
- Operational impact analysis
- Risk assessment
- Priority matrix for fixes
- Success criteria

**Use when:** You need to understand the business impact or prioritize fixes

---

### 5. [Unknowns and Evidence Gaps](./UNKNOWNS.md)
**Purpose:** Investigation roadmap

**Contains:**
- 6 categorized unknowns
- What we know vs what we don't know
- Evidence gaps identified
- Investigation commands
- Estimated effort for each unknown

**Use when:** You need to continue root cause investigation

---

## How to Use These Files

### If You're Investigating an Issue

1. **Start with:** [Issues List](./ISSUES_LIST.md)
   - Understand the issue category and symptoms

2. **Review:** [Reproduction Evidence](./REPRODUCTION_EVIDENCE.md)
   - Get raw logs and error messages
   - Follow reproduction steps

3. **Check:** [Signal Correlation](./SIGNAL_CORRELATION.md)
   - Trace the signal flow to find the breaking point

4. **Consult:** [Unknowns](./UNKNOWNS.md)
   - See what evidence is still needed
   - Follow investigation commands

5. **Reference:** [Impact Assessment](./IMPACT_ASSESSMENT.md)
   - Understand the priority and business impact

### If You're Planning Fixes

1. **Review:** [Impact Assessment](./IMPACT_ASSESSMENT.md)
   - Check priority matrix
   - Understand success criteria

2. **Address Unknowns:** [Unknowns](./UNKNOWNS.md)
   - Gather needed evidence
   - Fill evidence gaps

3. **Verify Fixes:** [Reproduction Evidence](./REPRODUCTION_EVIDENCE.md)
   - Use reproduction steps to verify
   - Confirm success with success criteria

### If You're Writing a Root Cause Analysis

1. **Read All Files:** Complete understanding of the issue
2. **Fill Unknowns:** Use investigation commands in [Unknowns](./UNKNOWNS.md)
3. **Cross-Reference:** Use [Signal Correlation](./SIGNAL_CORRELATION.md) to trace failures
4. **Document:** Add findings to appropriate files

---

## Quick Facts

### Container Status
- **k12-backend:** Up ~1 hour (since 14:23:23)
- **k12-signoz-otel-collector:** Up ~1 hour (since 14:26:14)
- **k12-postgres:** Up ~1 hour

### Network Status
- Both containers on: `k12-signoz-net` (10.89.10.x)
- DNS resolution: Working
- TCP connectivity: Confirmed
- HTTP endpoints: Accessible

### Data in ClickHouse
- **Traces:** 0 rows
- **Metrics:** 0 rows
- **Logs:** 12 rows (all old, from dev mode)

### Export Status
- **Traces:** Failing (error every 5-15s)
- **Metrics:** Stopped (no scraping for 59 min)
- **Logs:** Not exporting (protocol mismatch)

---

## Related Files

### Main Report
- [Complete Characterization Report](../TELEMETRY_ISSUES_CHARACTERIZATION.md)
  - Consolidated version of all issues
  - Executive summary

### Debugging Documentation
- [Docker Debugging Summary](../DOCKER_DEBUGGING_SUMMARY.md)
  - Previous debugging attempts
  - Docker setup details

### Configuration Files
- [otel-collector-config.yaml](../signoz-otel-collector-config.yaml)
  - Prometheus scrape configuration
- [docker-compose.yml](../docker-compose.yml)
  - Container configuration

---

## Next Steps

1. **Choose an issue** based on priority matrix in [Impact Assessment](./IMPACT_ASSESSMENT.md)
2. **Investigate unknowns** using commands in [Unknowns](./UNKNOWNS.md)
3. **Gather evidence** as outlined in [Reproduction Evidence](./REPRODUCTION_EVIDENCE.md)
4. **Implement fix** and verify using reproduction steps
5. **Update these files** with findings

---

## Version History

- **2026-02-25 15:25:39** - Initial characterization created
- Issues identified and categorized
- Evidence collected and documented
- Unknowns identified and catalogued
