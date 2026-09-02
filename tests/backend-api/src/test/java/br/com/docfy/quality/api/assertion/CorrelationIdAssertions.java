package br.com.docfy.quality.api.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.restassured.response.Response;
import java.util.UUID;

public final class CorrelationIdAssertions {

  private static final String HEADER_NAME = "X-Correlation-ID";

  private CorrelationIdAssertions() {}

  public static String assertValidCorrelationId(Response response) {
    String correlationId = response.header(HEADER_NAME);
    assertThat(correlationId).as("response header %s", HEADER_NAME).isNotBlank();

    try {
      UUID.fromString(correlationId);
    } catch (IllegalArgumentException exception) {
      fail("Expected %s to contain a UUID, but was <%s>", HEADER_NAME, correlationId);
    }

    return correlationId;
  }

  public static void assertEchoedCorrelationId(Response response, String expected) {
    assertThat(assertValidCorrelationId(response))
        .as("correlation ID propagated by the API")
        .isEqualTo(expected);
  }
}
