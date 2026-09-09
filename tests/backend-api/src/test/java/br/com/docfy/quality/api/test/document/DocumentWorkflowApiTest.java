package br.com.docfy.quality.api.test.document;

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
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Epic("Backend API")
@Feature("Document workflow")
@Tag("workflow")
@Tag("regression")
@Tag("requires-seed")
class DocumentWorkflowApiTest {

  private final AuthenticationFlow authentication = new AuthenticationFlow();
  private final DocumentFlow documentFlow = new DocumentFlow();
  private final DocumentsApiClient documentsApi = new DocumentsApiClient();

  @Test
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Administrator completes the lifecycle of a collaborator document")
  void administratorShouldCompleteDocumentLifecycle() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String administratorToken = authentication.accessTokenFor(SeededUser.ADMIN);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Complete lifecycle"), "Workflow validation");

    Response submission = documentsApi.submit(administratorToken, document.id());
    assertSuccessfulTransition(submission, document.id(), "IN_REVIEW");

    Response approval = documentsApi.approve(administratorToken, document.id());
    assertSuccessfulTransition(approval, document.id(), "APPROVED");

    Response archive = documentsApi.archive(administratorToken, document.id());
    assertSuccessfulTransition(archive, document.id(), "ARCHIVED");

    Response persisted = documentsApi.getById(ownerToken, document.id());
    persisted
        .then()
        .statusCode(200)
        .body("id", equalTo(document.id().toString()))
        .body("documentCode", equalTo(document.documentCode()))
        .body("createdBy", equalTo(document.createdBy().toString()))
        .body("status", equalTo("ARCHIVED"));
  }

  @Test
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Collaborator submits a document created by the same user")
  void collaboratorShouldSubmitOwnDraft() {
    String collaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture document =
        documentFlow.createDraft(
            collaboratorToken, uniqueTitle("Own submission"), "Workflow validation");

    Response submission = documentsApi.submit(collaboratorToken, document.id());

    assertSuccessfulTransition(submission, document.id(), "IN_REVIEW");
    assertPersistedStatus(collaboratorToken, document.id(), "IN_REVIEW");
  }

  @Test
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Collaborator cannot discover or submit another collaborator draft")
  void collaboratorShouldNotSubmitAnotherCollaboratorDraft() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String otherCollaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_JOAO);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Private submission"), "Workflow validation");

    Response submission = documentsApi.submit(otherCollaboratorToken, document.id());

    assertThatApiError(submission)
        .hasStatus(404)
        .hasMessage("Document not found")
        .hasPath(workflowPath(document.id(), "submit"))
        .hasConsistentCorrelationId();
    assertPersistedStatus(ownerToken, document.id(), "DRAFT");
  }

  @Test
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Manager rejection returns a document under review to draft")
  void managerShouldRejectDocumentUnderReview() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Manager rejection"), "Workflow validation");
    documentFlow.submit(ownerToken, document.id());

    Response rejection = documentsApi.reject(managerToken, document.id());

    assertSuccessfulTransition(rejection, document.id(), "DRAFT");
    assertPersistedStatus(ownerToken, document.id(), "DRAFT");
  }

  @ParameterizedTest(name = "collaborator cannot {0} a document under review")
  @MethodSource("restrictedReviewActions")
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Collaborator cannot approve or reject documents")
  void collaboratorShouldNotReviewDocument(String action) {
    String collaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture document =
        documentFlow.createDraft(
            collaboratorToken, uniqueTitle("Restricted review"), "Workflow validation");
    documentFlow.submit(collaboratorToken, document.id());

    Response review = performAction(action, collaboratorToken, document.id());

    assertThatApiError(review)
        .hasStatus(403)
        .hasMessage("Forbidden")
        .hasPath(workflowPath(document.id(), action))
        .hasConsistentCorrelationId();
    assertPersistedStatus(collaboratorToken, document.id(), "IN_REVIEW");
  }

  @Test
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Collaborator cannot archive another user's visible approved document")
  void collaboratorShouldNotArchiveVisibleApprovedDocument() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_JOAO);
    String collaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Protected archive"), "Workflow validation");
    documentFlow.submit(ownerToken, document.id());
    documentFlow.approve(managerToken, document.id());

    Response archive = documentsApi.archive(collaboratorToken, document.id());

    assertThatApiError(archive)
        .hasStatus(403)
        .hasMessage("Forbidden")
        .hasPath(workflowPath(document.id(), "archive"))
        .hasConsistentCorrelationId();
    assertPersistedStatus(managerToken, document.id(), "APPROVED");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidTransitions")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Invalid lifecycle transitions are rejected without changing status")
  void invalidTransitionShouldBeRejected(
      String scenario, String initialStatus, String action, String targetStatus) {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Invalid transition"), "Workflow validation");

    if (initialStatus.equals("IN_REVIEW")) {
      documentFlow.submit(ownerToken, document.id());
    }

    Response transition = performAction(action, managerToken, document.id());

    assertThatApiError(transition)
        .hasStatus(409)
        .hasMessage(
            "Invalid document status transition from " + initialStatus + " to " + targetStatus)
        .hasPath(workflowPath(document.id(), action))
        .hasConsistentCorrelationId();
    assertPersistedStatus(managerToken, document.id(), initialStatus);
  }

  private static Stream<Arguments> restrictedReviewActions() {
    return Stream.of(Arguments.of("approve"), Arguments.of("reject"));
  }

  private static Stream<Arguments> invalidTransitions() {
    return Stream.of(
        Arguments.of("draft cannot be approved", "DRAFT", "approve", "APPROVED"),
        Arguments.of("draft cannot be rejected", "DRAFT", "reject", "DRAFT"),
        Arguments.of("draft cannot be archived", "DRAFT", "archive", "ARCHIVED"),
        Arguments.of(
            "document under review cannot be submitted again", "IN_REVIEW", "submit", "IN_REVIEW"));
  }

  private Response performAction(String action, String accessToken, UUID documentId) {
    return switch (action) {
      case "submit" -> documentsApi.submit(accessToken, documentId);
      case "approve" -> documentsApi.approve(accessToken, documentId);
      case "reject" -> documentsApi.reject(accessToken, documentId);
      case "archive" -> documentsApi.archive(accessToken, documentId);
      default -> throw new IllegalArgumentException("Unsupported workflow action: " + action);
    };
  }

  private void assertSuccessfulTransition(
      Response response, UUID documentId, String expectedStatus) {
    response
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/document-response.schema.json"))
        .body("id", equalTo(documentId.toString()))
        .body("status", equalTo(expectedStatus));
    assertValidCorrelationId(response);
  }

  private void assertPersistedStatus(String accessToken, UUID documentId, String expectedStatus) {
    documentsApi
        .getById(accessToken, documentId)
        .then()
        .statusCode(200)
        .body("status", equalTo(expectedStatus));
  }

  private String workflowPath(UUID documentId, String action) {
    return "/api/v1/documents/" + documentId + "/" + action;
  }

  private String uniqueTitle(String prefix) {
    return prefix + " " + UUID.randomUUID();
  }
}
