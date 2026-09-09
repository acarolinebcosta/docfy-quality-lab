package br.com.docfy.quality.api.specification;

import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.LogConfig.logConfig;

import br.com.docfy.quality.api.config.TestConfiguration;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public final class RequestSpecifications {

  private RequestSpecifications() {}

  public static RequestSpecification defaultRequest() {
    int timeout = TestConfiguration.httpTimeoutMilliseconds();
    RestAssuredConfig config =
        RestAssuredConfig.config()
            .logConfig(
                logConfig()
                    .blacklistDefaultSensitiveHeaders()
                    .blacklistHeader("Authorization")
                    .enableLoggingOfRequestAndResponseIfValidationFails())
            .httpClient(
                httpClientConfig()
                    .setParam("http.connection.timeout", timeout)
                    .setParam("http.socket.timeout", timeout));

    return new RequestSpecBuilder()
        .setBaseUri(TestConfiguration.baseUrl())
        .setAccept(ContentType.JSON)
        .setConfig(config)
        .build();
  }

  public static RequestSpecification jsonRequest() {
    return new RequestSpecBuilder()
        .addRequestSpecification(defaultRequest())
        .setContentType(ContentType.JSON)
        .build();
  }

  public static RequestSpecification authenticated(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      throw new IllegalArgumentException("Access token must not be blank");
    }

    return new RequestSpecBuilder()
        .addRequestSpecification(defaultRequest())
        .addHeader("Authorization", "Bearer " + accessToken)
        .build();
  }

  public static RequestSpecification authenticatedJson(String accessToken) {
    return new RequestSpecBuilder()
        .addRequestSpecification(authenticated(accessToken))
        .setContentType(ContentType.JSON)
        .build();
  }
}
