package br.com.docfy.quality.api.test.security;

import static br.com.docfy.quality.api.assertion.ApiErrorAssertions.assertThatApiError;
import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.docfy.quality.api.client.AuthenticationApiClient;
import br.com.docfy.quality.api.client.CategoriesApiClient;
import br.com.docfy.quality.api.client.DocumentsApiClient;
import br.com.docfy.quality.api.config.TestConfiguration;
import br.com.docfy.quality.api.model.request.CreateDocumentRequest;
import br.com.docfy.quality.api.model.request.LoginRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Backend API")
@Feature("Document authorization")
@Tag("security")
@Tag("rbac")
class DocumentAuthorizationTest {

  private final AuthenticationApiClient authenticationApi = new AuthenticationApiClient();

  private final CategoriesApiClient categoriesApi = new CategoriesApiClient();

  private final DocumentsApiClient documentsApi = new DocumentsApiClient();

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Collaborator can submit a document created by the same user")
  void collaboratorShouldSubmitOwnDocument() {
    String collaboratorToken = login("ana@docfy.local");

    UUID documentId = createDraftDocument(collaboratorToken);

    Response response = documentsApi.submit(collaboratorToken, documentId);

    response
        .then()
        .statusCode(200)
        .body("id", equalTo(documentId.toString()))
        .body("status", equalTo("IN_REVIEW"));

    assertValidCorrelationId(response);
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Collaborator cannot approve a document")
  void collaboratorShouldNotApproveDocument() {
    String collaboratorToken = login("ana@docfy.local");

    UUID documentId = createDraftDocument(collaboratorToken);

    documentsApi.submit(collaboratorToken, documentId).then().statusCode(200);

    Response response = documentsApi.approve(collaboratorToken, documentId);

    assertThatApiError(response)
        .hasStatus(403)
        .hasPath("/api/v1/documents/" + documentId + "/approve")
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Collaborator cannot read another collaborator draft document")
  void collaboratorShouldNotReadAnotherCollaboratorDraft() {
    String ownerToken = login("ana@docfy.local");
    String otherCollaboratorToken = login("joao@docfy.local");

    UUID documentId = createDraftDocument(ownerToken);

    Response response = documentsApi.getById(otherCollaboratorToken, documentId);

    assertThatApiError(response)
        .hasStatus(404)
        .hasPath("/api/v1/documents/" + documentId)
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Manager can read collaborator draft document")
  void managerShouldReadCollaboratorDraft() {
    String collaboratorToken = login("ana@docfy.local");
    String managerToken = login("manager@docfy.local");

    UUID documentId = createDraftDocument(collaboratorToken);

    Response response = documentsApi.getById(managerToken, documentId);

    response
        .then()
        .statusCode(200)
        .body("id", equalTo(documentId.toString()))
        .body("status", equalTo("DRAFT"));

    assertValidCorrelationId(response);
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Manager can approve a document under review")
  void managerShouldApproveDocumentUnderReview() {
    String collaboratorToken = login("ana@docfy.local");

    String managerToken = login("manager@docfy.local");

    UUID documentId = createDraftDocument(collaboratorToken);

    documentsApi.submit(collaboratorToken, documentId).then().statusCode(200);

    Response response = documentsApi.approve(managerToken, documentId);

    response
        .then()
        .statusCode(200)
        .body("id", equalTo(documentId.toString()))
        .body("status", equalTo("APPROVED"));

    assertValidCorrelationId(response);
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Manager can reject a document under review")
  void managerShouldRejectDocumentUnderReview() {
    String collaboratorToken = login("ana@docfy.local");

    String managerToken = login("manager@docfy.local");

    UUID documentId = createDraftDocument(collaboratorToken);

    documentsApi.submit(collaboratorToken, documentId).then().statusCode(200);

    Response response = documentsApi.reject(managerToken, documentId);

    response
        .then()
        .statusCode(200)
        .body("id", equalTo(documentId.toString()))
        .body("status", equalTo("DRAFT"));

    assertValidCorrelationId(response);
  }

  private UUID createDraftDocument(String token) {
    UUID categoryId = firstCategoryId(token);

    Response response =
        documentsApi.create(
            token,
            new CreateDocumentRequest(
                "RBAC document " + UUID.randomUUID(), "Created for RBAC validation", categoryId));

    response.then().statusCode(201).body("status", equalTo("DRAFT"));

    return UUID.fromString(response.jsonPath().getString("id"));
  }

  private String login(String email) {
    Optional<String> configuredPassword = TestConfiguration.testPassword();

    assumeTrue(
        configuredPassword.isPresent(), "Set DOCFY_TEST_PASSWORD to execute seeded user scenarios");

    Response response =
        authenticationApi.login(new LoginRequest(email, configuredPassword.orElseThrow()));

    response.then().statusCode(200);

    return response.jsonPath().getString("accessToken");
  }

  private UUID firstCategoryId(String token) {
    Response response = categoriesApi.listWithToken(token);

    response.then().statusCode(200);

    return UUID.fromString(response.jsonPath().getString("[0].id"));
  }
}
