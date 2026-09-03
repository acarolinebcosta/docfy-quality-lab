package br.com.docfy.quality.api.test.security;

import static br.com.docfy.quality.api.assertion.ApiErrorAssertions.assertThatApiError;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import br.com.docfy.quality.api.client.CategoriesApiClient;
import br.com.docfy.quality.api.client.DocumentsApiClient;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Epic("Backend API")
@Feature("Authentication boundary")
@Tag("security")
class AuthenticationBoundaryTest {

  private final CategoriesApiClient categoriesApi = new CategoriesApiClient();
  private final DocumentsApiClient documentsApi = new DocumentsApiClient();

  @ParameterizedTest(name = "{index} - /api/v1/{0}")
  @ValueSource(strings = {"categories", "documents"})
  @Tag("smoke")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Protected resources reject requests without a bearer token")
  void shouldRejectRequestWithoutToken(String resource) {
    Response response = callWithoutAuthentication(resource);

    response.then().body(matchesJsonSchemaInClasspath("schemas/api-error-response.schema.json"));
    assertThatApiError(response)
        .hasStatus(401)
        .hasPath("/api/v1/" + resource)
        .hasConsistentCorrelationId();
  }

  @ParameterizedTest(name = "{index} - /api/v1/{0}")
  @ValueSource(strings = {"categories", "documents"})
  @Tag("regression")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Protected resources reject an invalid bearer token")
  void shouldRejectInvalidToken(String resource) {
    Response response = callWithInvalidToken(resource);

    response.then().body(matchesJsonSchemaInClasspath("schemas/api-error-response.schema.json"));
    assertThatApiError(response)
        .hasStatus(401)
        .hasPath("/api/v1/" + resource)
        .hasConsistentCorrelationId();
  }

  private Response callWithoutAuthentication(String resource) {
    return switch (resource) {
      case "categories" -> categoriesApi.listWithoutAuthentication();
      case "documents" -> documentsApi.listWithoutAuthentication();
      default -> throw new IllegalArgumentException("Unsupported resource: " + resource);
    };
  }

  private Response callWithInvalidToken(String resource) {
    return switch (resource) {
      case "categories" -> categoriesApi.listWithToken("invalid-token");
      case "documents" -> documentsApi.listWithToken("invalid-token");
      default -> throw new IllegalArgumentException("Unsupported resource: " + resource);
    };
  }
}
