const DEFAULT_BASE_URL = "http://localhost:8080";
const DEFAULT_USER_EMAIL = "manager@docfy.local";
const LOOPBACK_TARGET = /^https?:\/\/(localhost|127\.0\.0\.1|\[::1\])(?::\d+)?$/i;

function normalizeBaseUrl(value) {
  return value.replace(/\/+$/, "");
}

function numberFromEnvironment(name, fallback) {
  const rawValue = __ENV[name];

  if (!rawValue) {
    return fallback;
  }

  const parsed = Number(rawValue);

  if (!Number.isFinite(parsed) || parsed < 0) {
    throw new Error(`${name} must be a non-negative number`);
  }

  return parsed;
}

export function runtimeConfig() {
  const baseUrl = normalizeBaseUrl(__ENV.DOCFY_API_BASE_URL || DEFAULT_BASE_URL);
  const remoteTargetAllowed = __ENV.K6_ALLOW_REMOTE_TARGET === "true";

  if (!LOOPBACK_TARGET.test(baseUrl) && !remoteTargetAllowed) {
    throw new Error(
      "Remote performance targets require the explicit K6_ALLOW_REMOTE_TARGET=true safeguard",
    );
  }

  return {
    baseUrl,
    password: __ENV.DOCFY_TEST_PASSWORD || "",
    userEmail: __ENV.DOCFY_PERFORMANCE_USER_EMAIL || DEFAULT_USER_EMAIL,
    requestTimeout: __ENV.K6_HTTP_TIMEOUT || "10s",
    thinkTimeSeconds: numberFromEnvironment("K6_THINK_TIME_SECONDS", 1),
  };
}
