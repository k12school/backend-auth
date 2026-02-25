# 🧪 PROMPT 3 — Forensic Review of Previous Attempts

---

## 📌 Instruction to Agent

Your task is to perform a forensic analysis of all prior attempts to fix logging and metrics issues.

Do not retry fixes yet.
Do not suggest new ones yet.

You must:

* Extract Git history related to observability
* Review config changes
* Review docker-compose diffs
* Review environment variable changes
* Review library upgrades
* Review dependency additions/removals

---

## 🔎 Step 1 — Extract Observability-Related Changes

Search Git history for:

* logging
* logback
* log4j
* micrometer
* prometheus
* opentelemetry
* otel
* jaeger
* zipkin
* collector
* exporter
* metrics
* tracing

Produce timeline table:

| Date | Commit | Change | Intended Fix | Result |

---

## 🔎 Step 2 — Validate Each Attempt

For each attempt:

* Was it correctly configured?
* Was it fully applied?
* Were containers rebuilt?
* Were env vars correct?
* Was the collector reachable?
* Did version incompatibility exist?

Classify failure reason:

* Misconfiguration
* Wrong assumption
* Version mismatch
* Network isolation
* Resource exhaustion
* Partial deployment
* Not reproducible
* Actually working but misinterpreted

---

## 🔎 Step 3 — Root Pattern Detection

Detect patterns such as:

* Fix applied only in app but not collector
* Metrics added but scrape config missing
* Spans annotated but no exporter configured
* Log format changed but parser not updated
* Port exposed but wrong network

---

## 📦 Required Output

1. Attempt Timeline
2. Failure Classification Matrix
3. Recurring Mistake Patterns
4. Verified Working vs Actually Broken
5. Remaining Hypotheses (without fixing yet)

**Goal:** Understand what was already tried and why it failed.

Stop after report.