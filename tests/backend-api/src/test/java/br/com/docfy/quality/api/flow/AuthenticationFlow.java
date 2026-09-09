package br.com.docfy.quality.api.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.docfy.quality.api.client.AuthenticationApiClient;
import br.com.docfy.quality.api.config.TestConfiguration;
import br.com.docfy.quality.api.model.SeededUser;
import br.com.docfy.quality.api.model.request.LoginRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import java.util.Optional;

public final class AuthenticationFlow {

  private final AuthenticationApiClient authenticationApi = new AuthenticationApiClient();

  @Step("Authenticate seeded user {user}")
  public String accessTokenFor(SeededUser user) {
    Optional<String> configuredPassword = TestConfiguration.testPassword();
    assumeTrue(
        configuredPassword.isPresent(), "Set DOCFY_TEST_PASSWORD to execute seeded user scenarios");

    Response response =
        authenticationApi.login(new LoginRequest(user.email(), configuredPassword.orElseThrow()));

    assertThat(response.statusCode()).as("login precondition for %s", user).isEqualTo(200);

    String accessToken = response.jsonPath().getString("accessToken");
    assertThat(accessToken).as("access token for %s", user).isNotBlank();
    return accessToken;
  }
}
