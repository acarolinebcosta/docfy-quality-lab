package br.com.docfy.quality.api.test.authentication;

import static br.com.docfy.quality.api.assertion.ApiErrorAssertions.assertThatApiError;
import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.docfy.quality.api.client.AuthenticationApiClient;
import br.com.docfy.quality.api.client.DocumentsApiClient;
import br.com.docfy.quality.api.config.TestConfiguration;
import br.com.docfy.quality.api.model.request.LoginRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Epic("Backend API")
@Feature("Authentication")
@Tag("authentication")
class AuthenticationApiTest {

  private final AuthenticationApiClient authenticationApi = new AuthenticationApiClient();
  private final DocumentsApiClient documentsApi = new DocumentsApiClient();

  @ParameterizedTest(name = "{index} - {1}")
  @MethodSource("seededUsers")
  @Tag("smoke")
  @Tag("requires-seed")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Seeded users receive a token accepted by a protected resource")
  void shouldAuthenticateSeededUser(String email, String role) {
    Optional<String> configuredPassword = TestConfiguration.testPassword();
    assumeTrue(
        configuredPassword.isPresent(),
        "Set DOCFY_TEST_PASSWORD to execute authentication with seeded users");

    Response loginResponse =
        authenticationApi.login(new LoginRequest(email, configuredPassword.orElseThrow()));

    loginResponse
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/login-response.schema.json"))
        .body("accessToken", not(blankOrNullString()))
        .body("tokenType", equalTo("Bearer"))
        .body("expiresIn", greaterThan(0));
    assertValidCorrelationId(loginResponse);

    String accessToken = loginResponse.jsonPath().getString("accessToken");
    Response protectedResponse = documentsApi.listWithToken(accessToken);

    assertThat(protectedResponse.statusCode())
        .as("documents access for seeded role %s", role)
        .isEqualTo(200);
    assertValidCorrelationId(protectedResponse);
  }

  @Test
  @Tag("regression")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Invalid credentials are rejected with the standard error contract")
  void shouldRejectInvalidCredentials() {
    LoginRequest invalidCredentials =
        new LoginRequest("admin@docfy.local", "invalid-" + UUID.randomUUID());

    Response response = authenticationApi.login(invalidCredentials);

    response.then().body(matchesJsonSchemaInClasspath("schemas/api-error-response.schema.json"));
    assertThatApiError(response)
        .hasStatus(401)
        .hasPath("/api/v1/auth/login")
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("contract")
  @Tag("regression")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Malformed login payload is rejected as a client error")
  void shouldRejectMalformedLoginPayload() {
    Response response = authenticationApi.login(new LoginRequest("not-an-email", ""));

    response.then().body(matchesJsonSchemaInClasspath("schemas/api-error-response.schema.json"));
    assertThatApiError(response)
        .hasStatus(400)
        .hasPath("/api/v1/auth/login")
        .hasConsistentCorrelationId();
  }

  static Stream<Arguments> seededUsers() {
    return Stream.of(
        Arguments.of("admin@docfy.local", "ADMIN"),
        Arguments.of("manager@docfy.local", "MANAGER"),
        Arguments.of("ana@docfy.local", "COLLABORATOR"),
        Arguments.of("joao@docfy.local", "COLLABORATOR"));
  }
}
