package br.com.docfy.quality.api.assertion;

import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;

public final class ApiErrorAssertions {

  private final Response response;

  private ApiErrorAssertions(Response response) {
    this.response = response;
  }

  public static ApiErrorAssertions assertThatApiError(Response response) {
    response.then().body(matchesJsonSchemaInClasspath("schemas/api-error-response.schema.json"));
    return new ApiErrorAssertions(response);
  }

  public ApiErrorAssertions hasStatus(int expectedStatus) {
    assertThat(response.statusCode()).as("HTTP status").isEqualTo(expectedStatus);
    assertThat(response.jsonPath().getInt("status"))
        .as("error body status")
        .isEqualTo(expectedStatus);
    return this;
  }

  public ApiErrorAssertions hasPath(String expectedPath) {
    assertThat(response.jsonPath().getString("path")).as("error body path").isEqualTo(expectedPath);
    return this;
  }

  public ApiErrorAssertions hasMessage(String expectedMessage) {
    assertThat(response.jsonPath().getString("message"))
        .as("error body message")
        .isEqualTo(expectedMessage);
    return this;
  }

  public ApiErrorAssertions hasConsistentCorrelationId() {
    String headerCorrelationId = assertValidCorrelationId(response);
    assertThat(response.jsonPath().getString("correlationId"))
        .as("correlation ID in the error body")
        .isEqualTo(headerCorrelationId);
    return this;
  }
}
