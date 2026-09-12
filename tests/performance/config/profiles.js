const OPERATIONS = [
  "categories_list",
  "documents_list",
  "document_detail",
  "document_audit",
  "document_files",
];

function thresholds({ checks, failures, p95, p99, emergencyFailureRate }) {
  const failureThresholds = [`rate<${failures}`];

  if (emergencyFailureRate) {
    failureThresholds.unshift({
      threshold: `rate<${emergencyFailureRate}`,
      abortOnFail: true,
      delayAbortEval: "20s",
    });
  }

  const gates = {
    "checks{scenario:document_catalog}": [`rate>${checks}`],
    "http_req_failed{scenario:document_catalog}": failureThresholds,
    "http_req_duration{scenario:document_catalog}": [
      `p(95)<${p95}`,
      `p(99)<${p99}`,
    ],
  };

  for (const operation of OPERATIONS) {
    gates[`http_req_duration{operation:${operation}}`] = [`p(95)<${p95}`];
  }

  return gates;
}

const commonOptions = {
  tags: {
    system: "docfy",
    test_track: "performance",
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
};

export const profiles = {
  smoke: {
    label: "Performance smoke",
    objective: "Validate the workload and critical journey with minimal traffic.",
    workload: "1 VU, 5 shared iterations, up to 30 seconds",
    options: {
      ...commonOptions,
      scenarios: {
        document_catalog: {
          executor: "shared-iterations",
          vus: 1,
          iterations: 5,
          maxDuration: "30s",
          gracefulStop: "5s",
          tags: { profile: "smoke" },
        },
      },
      thresholds: thresholds({
        checks: 0.9999,
        failures: 0.0001,
        p95: 1000,
        p99: 1500,
      }),
    },
  },
  load: {
    label: "Reference load",
    objective: "Assess reliability and latency under the CI reference workload.",
    workload: "Ramp to 10 VUs, hold the reference load, then ramp down over 90 seconds",
    options: {
      ...commonOptions,
      scenarios: {
        document_catalog: {
          executor: "ramping-vus",
          startVUs: 0,
          stages: [
            { duration: "15s", target: 5 },
            { duration: "30s", target: 10 },
            { duration: "30s", target: 10 },
            { duration: "15s", target: 0 },
          ],
          gracefulRampDown: "5s",
          gracefulStop: "5s",
          tags: { profile: "load" },
        },
      },
      thresholds: thresholds({
        checks: 0.99,
        failures: 0.01,
        p95: 750,
        p99: 1500,
      }),
    },
  },
  stress: {
    label: "Controlled stress",
    objective: "Observe service behavior above the reference load with a safe abort guard.",
    workload: "Ramp to 35 VUs, hold controlled stress, then recover over 90 seconds",
    options: {
      ...commonOptions,
      scenarios: {
        document_catalog: {
          executor: "ramping-vus",
          startVUs: 0,
          stages: [
            { duration: "15s", target: 10 },
            { duration: "20s", target: 20 },
            { duration: "20s", target: 35 },
            { duration: "20s", target: 35 },
            { duration: "15s", target: 0 },
          ],
          gracefulRampDown: "5s",
          gracefulStop: "5s",
          tags: { profile: "stress" },
        },
      },
      thresholds: thresholds({
        checks: 0.95,
        failures: 0.05,
        emergencyFailureRate: 0.2,
        p95: 1500,
        p99: 3000,
      }),
    },
  },
};

export function profile(name) {
  const selected = profiles[name];

  if (!selected) {
    throw new Error(`Unknown performance profile: ${name}`);
  }

  return selected;
}
