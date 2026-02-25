# 🔥 PROMPT 2 — Current Issues Characterization

---

## 📌 Instruction to Agent

Now that the environment state is mapped, your task is to characterize the current logging and metrics issues.

Do not propose fixes yet.
Your job is to transform vague symptoms into measurable failures.

---

## 🔎 Step 1 — Define the Reported Issues

For each issue:

* Exact symptom
* When it happens
* Which container
* Which tool
* Frequency
* Is it intermittent or constant?

Classify each issue:

* Missing logs
* Delayed logs
* Incorrect log level
* Logs not collected
* Metrics missing
* Metrics stale
* Wrong labels
* Scrape failures
* Exporter failures
* Trace gaps
* Performance-related observability gaps

---

## 🔎 Step 2 — Reproduce & Validate

For each issue:

* Reproduce it
* Capture logs during reproduction
* Capture metrics endpoint response
* Capture Prometheus target state
* Capture OTEL exporter logs

Attach:

* Raw log snippets
* HTTP responses
* Error messages
* Timestamps correlation

---

## 🔎 Step 3 — Signal Correlation

Determine:

* Are logs being produced but not collected?
* Are metrics being exposed but not scraped?
* Are traces created but not exported?
* Are network issues involved?
* Are resource constraints affecting telemetry?

Provide a correlation matrix:

| Issue | Producing Component | Transport Layer | Collector | Storage | UI | Status |

---

## 📦 Required Output

Structured issue dossier:

1. Issue List
2. Reproduction Evidence
3. Correlation Matrix
4. Impact Assessment
5. Unknowns / Gaps

**Goal:** Define precisely what is failing and how.

Stop after report.