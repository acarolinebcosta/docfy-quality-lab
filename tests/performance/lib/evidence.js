import { profile } from "../config/profiles.js";

function metric(data, name) {
  return data.metrics[name]?.values || {};
}

function formatNumber(value, digits = 2) {
  return Number.isFinite(value) ? Number(value).toFixed(digits) : "n/a";
}

function formatRate(value) {
  return Number.isFinite(value) ? `${(value * 100).toFixed(2)}%` : "n/a";
}

function thresholdRows(data) {
  const rows = [];

  for (const [metricName, metricData] of Object.entries(data.metrics)) {
    for (const [expression, result] of Object.entries(metricData.thresholds || {})) {
      rows.push(
        `| \`${metricName}\` | \`${expression}\` | ${result.ok ? "PASS" : "FAIL"} |`,
      );
    }
  }

  return rows.sort();
}

function executionCompleted(data) {
  const checks = metric(data, "checks{scenario:document_catalog}");
  const iterations = metric(data, "iterations");
  const executedChecks = (checks.passes || 0) + (checks.fails || 0);

  return executedChecks > 0 && (iterations.count || 0) > 0;
}

function gatePassed(data) {
  const thresholdResults = Object.values(data.metrics).flatMap((metricData) =>
    Object.values(metricData.thresholds || {}),
  );

  return (
    executionCompleted(data) &&
    thresholdResults.length > 0 &&
    thresholdResults.every((result) => result.ok !== false)
  );
}

function markdownSummary(data, profileName) {
  const selectedProfile = profile(profileName);
  const scenarioSuffix = "{scenario:document_catalog}";
  const checks = metric(data, `checks${scenarioSuffix}`);
  const duration = metric(data, `http_req_duration${scenarioSuffix}`);
  const failed = metric(data, `http_req_failed${scenarioSuffix}`);
  const requests = metric(data, "http_reqs");
  const iterations = metric(data, "iterations");
  const vusMax = metric(data, "vus_max");
  const rows = thresholdRows(data);
  const completed = executionCompleted(data);
  const status = gatePassed(data) ? "PASS" : "FAIL";

  return [
    "# Docfy Performance Quality Gate",
    "",
    "## Execution",
    "",
    "| Field | Value |",
    "| --- | --- |",
    `| Profile | ${selectedProfile.label} |`,
    `| Objective | ${selectedProfile.objective} |`,
    `| Workload | ${selectedProfile.workload} |`,
    `| Quality Lab SHA | \`${__ENV.QUALITY_LAB_SHA || "local"}\` |`,
    `| Docfy SUT SHA | \`${__ENV.DOCFY_SUT_SHA || "local"}\` |`,
    `| Environment | ${__ENV.CI_ENVIRONMENT || "local"} |`,
    `| Base URL | ${__ENV.DOCFY_API_BASE_URL || "http://localhost:8080"} |`,
    "",
    "## Results",
    "",
    "| Metric | Result |",
    "| --- | ---: |",
    `| Virtual users (max) | ${formatNumber(vusMax.max, 0)} |`,
    `| Iterations | ${formatNumber(iterations.count, 0)} |`,
    `| HTTP requests | ${formatNumber(requests.count, 0)} |`,
    `| Test duration | ${formatNumber(data.state?.testRunDurationMs)} ms |`,
    `| Check success rate | ${formatRate(checks.rate)} |`,
    `| HTTP failure rate | ${formatRate(failed.rate)} |`,
    `| Response time p95 | ${formatNumber(duration["p(95)"])} ms |`,
    `| Response time p99 | ${formatNumber(duration["p(99)"])} ms |`,
    "",
    "## Thresholds",
    "",
    "| Metric | Gate | Result |",
    "| --- | --- | --- |",
    `| \`execution\` | \`iterations>0 and checks>0\` | ${completed ? "PASS" : "FAIL"} |`,
    ...(rows.length > 0 ? rows : ["| No threshold evidence | n/a | FAIL |"]),
    "",
    `## Threshold result: ${status}`,
    "",
    "> These thresholds are repeatable engineering guardrails for the ephemeral CI environment; they are not production SLOs.",
    "",
  ].join("\n");
}

export function performanceSummary(data, profileName) {
  const markdown = markdownSummary(data, profileName);

  return {
    stdout: markdown,
    [`artifacts/${profileName}-summary.json`]: JSON.stringify(data, null, 2),
    [`artifacts/${profileName}-summary.md`]: markdown,
  };
}
