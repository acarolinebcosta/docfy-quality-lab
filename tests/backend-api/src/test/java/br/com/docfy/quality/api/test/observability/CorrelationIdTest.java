package br.com.docfy.quality.api.test.observability;

import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertEchoedCorrelationId;
import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.docfy.quality.api.client.PlatformApiClient;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Backend API")
@Feature("Observability")
@Tag("observability")
class CorrelationIdTest {

  private final PlatformApiClient platformApi = new PlatformApiClient();

  @Test
  @Tag("smoke")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Valid client correlation ID is propagated in the response")
  void shouldPropagateValidCorrelationId() {
    String correlationId = UUID.randomUUID().toString();

    Response response = platformApi.health(correlationId);

    assertThat(response.statusCode()).isEqualTo(200);
    assertEchoedCorrelationId(response, correlationId);
  }

  @Test
  @Tag("regression")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Invalid client correlation ID is replaced by a valid server ID")
  void shouldReplaceInvalidCorrelationId() {
    String invalidCorrelationId = "not-a-uuid";

    Response response = platformApi.health(invalidCorrelationId);

    assertThat(response.statusCode()).isEqualTo(200);
    String generatedCorrelationId = assertValidCorrelationId(response);
    assertThat(generatedCorrelationId).isNotEqualTo(invalidCorrelationId);
  }
}
