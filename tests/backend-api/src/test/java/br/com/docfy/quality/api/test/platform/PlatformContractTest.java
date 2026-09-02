package br.com.docfy.quality.api.test.platform;

import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.startsWith;

import br.com.docfy.quality.api.client.PlatformApiClient;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Backend API")
@Feature("Platform and API contract")
@Tag("contract")
class PlatformContractTest {

  private final PlatformApiClient platformApi = new PlatformApiClient();

  @Test
  @Tag("smoke")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Health endpoint reports that the Docfy API is available")
  void shouldReportHealthyApi() {
    Response response = platformApi.health();

    response
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/health-response.schema.json"))
        .body("status", equalTo("UP"));
    assertThat(response.contentType()).as("health response content type").contains("json");
    assertValidCorrelationId(response);
  }

  @Test
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Published OpenAPI document exposes the expected public contract")
  void shouldPublishExpectedOpenApiContract() {
    Response response = platformApi.openApi();

    response
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/openapi-response.schema.json"))
        .body("openapi", startsWith("3."))
        .body("info.title", equalTo("Docfy API"))
        .body("info.version", equalTo("v1"))
        .body("paths", hasKey("/api/v1/auth/login"))
        .body("paths", hasKey("/api/v1/documents"))
        .body("paths", hasKey("/api/v1/categories"))
        .body("components.securitySchemes.bearerAuth.type", equalTo("http"))
        .body("components.securitySchemes.bearerAuth.scheme", equalTo("bearer"))
        .body("components.securitySchemes.bearerAuth.bearerFormat", equalTo("JWT"));
    assertThat(response.contentType()).as("OpenAPI response content type").contains("json");
    assertValidCorrelationId(response);
  }
}
