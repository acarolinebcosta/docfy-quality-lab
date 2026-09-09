package br.com.docfy.quality.api.test.security;

import static br.com.docfy.quality.api.assertion.ApiErrorAssertions.assertThatApiError;
import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;

import br.com.docfy.quality.api.client.DocumentsApiClient;
import br.com.docfy.quality.api.flow.AuthenticationFlow;
import br.com.docfy.quality.api.flow.DocumentFixture;
import br.com.docfy.quality.api.flow.DocumentFlow;
import br.com.docfy.quality.api.model.SeededUser;
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
@Feature("Document visibility")
@Tag("security")
@Tag("rbac")
@Tag("requires-seed")
class DocumentAuthorizationTest {

  private final AuthenticationFlow authentication = new AuthenticationFlow();
  private final DocumentFlow documentFlow = new DocumentFlow();
  private final DocumentsApiClient documentsApi = new DocumentsApiClient();

  @Test
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Collaborator cannot read another collaborator draft document")
  void collaboratorShouldNotReadAnotherCollaboratorDraft() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String otherCollaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_JOAO);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, "Private document " + UUID.randomUUID(), "Visibility validation");

    Response response = documentsApi.getById(otherCollaboratorToken, document.id());

    assertThatApiError(response)
        .hasStatus(404)
        .hasMessage("Document not found")
        .hasPath("/api/v1/documents/" + document.id())
        .hasConsistentCorrelationId();
  }

  @Test
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Manager can read collaborator draft document")
  void managerShouldReadCollaboratorDraft() {
    String collaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(
            collaboratorToken, "Manager access " + UUID.randomUUID(), "Visibility validation");

    Response response = documentsApi.getById(managerToken, document.id());

    response
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/document-response.schema.json"))
        .body("id", equalTo(document.id().toString()))
        .body("status", equalTo("DRAFT"));
    assertValidCorrelationId(response);
  }
}
