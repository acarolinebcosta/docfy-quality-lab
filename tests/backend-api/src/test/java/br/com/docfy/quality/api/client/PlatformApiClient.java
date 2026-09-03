package br.com.docfy.quality.api.client;

import static br.com.docfy.quality.api.specification.RequestSpecifications.defaultRequest;
import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public final class PlatformApiClient {

  public Response health() {
    return given().spec(defaultRequest()).when().get("/actuator/health");
  }

  public Response health(String correlationId) {
    RequestSpecification request = given().spec(defaultRequest());
    if (correlationId != null) {
      request.header("X-Correlation-ID", correlationId);
    }
    return request.when().get("/actuator/health");
  }

  public Response openApi() {
    return given().spec(defaultRequest()).when().get("/v3/api-docs");
  }
}
