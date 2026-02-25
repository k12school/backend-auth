# 🧭 PROMPT 1 — Environment & Observability State Discovery

## 📌 Instruction to Agent

You are performing a full-state discovery of a Docker Compose–based application stack experiencing logging and metrics issues.

Do not diagnose yet.
Do not propose fixes yet.
Your task is to **map reality as-is**.

Work only with evidence from:

* docker-compose files (all merged configs)
* `.env` files
* container runtime state
* mounted volumes
* config files
* application configs
* logs
* metrics endpoints
* network configuration

If something is missing, explicitly state it.

---

## 🔎 Step 1 — Docker & Runtime State

Collect and present:

* `docker compose version`
* `docker version`
* `docker compose config` (fully merged)
* `docker ps -a`
* Restart counts
* Container health status
* Resource limits (CPU/memory)
* Network definitions
* Volumes definitions

Output as structured tables.

---

## 🔎 Step 2 — Logging Architecture Mapping

For each container:

* Logging driver in use
* Log destination (stdout, file, json-file, syslog, etc.)
* Mounted log volumes
* Log rotation configuration
* Log level configuration
* Structured logging enabled? (JSON? text?)
* Correlation IDs present?

Extract:

* Application logging framework (Logback? Log4j? JUL?)
* Logging config file contents
* Log format pattern
* Example raw log line

---

## 🔎 Step 3 — Metrics Architecture Mapping

Identify:

* Metrics libraries (Micrometer? OpenTelemetry? Dropwizard?)
* Metrics endpoints exposed (e.g., `/metrics`, `/q/metrics`, `/actuator/prometheus`)
* Prometheus scrape config
* OTEL collector config (if exists)
* Exporters configured (Prometheus, OTLP, Jaeger, Zipkin, etc.)
* Environment variables related to telemetry

Verify:

* Are metrics endpoints reachable from other containers?
* Are scrape targets UP?
* Are exporters reporting errors?

Collect evidence via:

* curl inside containers
* container logs
* config inspection

---

## 🔎 Step 4 — Tracing State (if present)

* Is OpenTelemetry enabled?
* Exporter endpoint configured?
* Sampling configuration?
* Are spans generated?
* Are spans exported?
* Any exporter errors in logs?

---

## 📦 Required Output Format

Produce a structured report:

1. Runtime Topology Diagram (textual)
2. Logging Flow Diagram (textual)
3. Metrics Flow Diagram (textual)
4. Observability Stack Inventory Table
5. Missing / Undefined Configurations
6. Suspicious Signals (but do NOT fix yet)

Everything must be evidence-based.

**Goal:** Build a complete, evidence-based map of the current Docker Compose observability stack.

Stop after report.