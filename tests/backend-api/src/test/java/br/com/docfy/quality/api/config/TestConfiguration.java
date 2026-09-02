package br.com.docfy.quality.api.config;

import java.util.Optional;

public final class TestConfiguration {

  private static final String DEFAULT_BASE_URL = "http://localhost:8080";
  private static final int DEFAULT_TIMEOUT_MS = 10_000;

  private TestConfiguration() {}

  public static String baseUrl() {
    String configured =
        read("docfy.base.url", "DOCFY_API_BASE_URL").orElse(DEFAULT_BASE_URL).trim();

    if (configured.isEmpty()) {
      throw new IllegalStateException("The Docfy API base URL must not be blank");
    }

    return configured.replaceAll("/+$", "");
  }

  public static Optional<String> testPassword() {
    return read("docfy.test.password", "DOCFY_TEST_PASSWORD")
        .map(String::trim)
        .filter(value -> !value.isEmpty());
  }

  public static int httpTimeoutMilliseconds() {
    String configured =
        read("docfy.http.timeout.ms", "DOCFY_HTTP_TIMEOUT_MS")
            .orElse(Integer.toString(DEFAULT_TIMEOUT_MS));

    try {
      int timeout = Integer.parseInt(configured);
      if (timeout <= 0) {
        throw new IllegalStateException("DOCFY_HTTP_TIMEOUT_MS must be greater than zero");
      }
      return timeout;
    } catch (NumberFormatException exception) {
      throw new IllegalStateException("DOCFY_HTTP_TIMEOUT_MS must be an integer", exception);
    }
  }

  private static Optional<String> read(String propertyName, String environmentName) {
    String propertyValue = System.getProperty(propertyName);
    if (propertyValue != null) {
      return Optional.of(propertyValue);
    }
    return Optional.ofNullable(System.getenv(environmentName));
  }
}
