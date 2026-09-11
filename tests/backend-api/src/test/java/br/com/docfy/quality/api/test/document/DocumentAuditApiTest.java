package br.com.docfy.quality.api.test.document;

import static br.com.docfy.quality.api.assertion.ApiErrorAssertions.assertThatApiError;
import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.docfy.quality.api.client.DocumentAuditApiClient;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Epic("Backend API")
@Feature("Document audit")
@Tag("audit")
@Tag("regression")
class DocumentAuditApiTest {

  private static final String AUDIT_SCHEMA = "schemas/document-audit-history-response.schema.json";

  private final AuthenticationFlow authentication = new AuthenticationFlow();
  private final DocumentFlow documentFlow = new DocumentFlow();
  private final DocumentsApiClient documentsApi = new DocumentsApiClient();
  private final DocumentAuditApiClient auditApi = new DocumentAuditApiClient();

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Newly created draft has no audit events")
  void newlyCreatedDraftShouldHaveEmptyAuditHistory() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(ownerToken, uniqueTitle("Empty audit"), "Audit trail validation");

    Response response = auditApi.getWithToken(managerToken, document.id());

    assertSuccessfulAuditResponse(response);
    assertThat(auditEvents(response)).as("new document audit history").isEmpty();
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Document submission creates an audit event with the request correlation ID")
  void submissionShouldCreateAuditEventWithRequestCorrelationId() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Submission audit"), "Audit trail validation");

    Response submission = documentsApi.submit(ownerToken, document.id());
    assertThat(submission.statusCode()).isEqualTo(200);
    String submissionCorrelationId = assertValidCorrelationId(submission);

    Response response = auditApi.getWithToken(managerToken, document.id());

    assertSuccessfulAuditResponse(response);

    List<Map<String, Object>> events = auditEvents(response);
    assertThat(events).hasSize(1);

    Map<String, Object> event = events.getFirst();
    assertAuditEvent(
        event, document.id(), "DOCUMENT_SUBMITTED", "DRAFT", "IN_REVIEW", submissionCorrelationId);

    assertThat(event.get("actorId"))
        .as("submission actor")
        .isEqualTo(document.createdBy().toString());
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Manager approval creates the expected audit event")
  void managerApprovalShouldCreateAuditEvent() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Approval audit"), "Audit trail validation");

    documentFlow.submit(ownerToken, document.id());

    Response approval = documentsApi.approve(managerToken, document.id());
    assertThat(approval.statusCode()).isEqualTo(200);
    String approvalCorrelationId = assertValidCorrelationId(approval);

    Response response = auditApi.getWithToken(managerToken, document.id());

    assertSuccessfulAuditResponse(response);

    List<Map<String, Object>> events = auditEvents(response);
    assertThat(events).hasSize(2);
    assertActions(events, "DOCUMENT_SUBMITTED", "DOCUMENT_APPROVED");

    Map<String, Object> approvalEvent = events.get(1);
    assertAuditEvent(
        approvalEvent,
        document.id(),
        "DOCUMENT_APPROVED",
        "IN_REVIEW",
        "APPROVED",
        approvalCorrelationId);

    assertThat(approvalEvent.get("actorId"))
        .as("approval actor must not be the document owner")
        .isNotEqualTo(document.createdBy().toString());
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Manager rejection creates the expected audit event")
  void managerRejectionShouldCreateAuditEvent() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Rejection audit"), "Audit trail validation");

    documentFlow.submit(ownerToken, document.id());

    Response rejection = documentsApi.reject(managerToken, document.id());
    assertThat(rejection.statusCode()).isEqualTo(200);
    String rejectionCorrelationId = assertValidCorrelationId(rejection);

    Response response = auditApi.getWithToken(managerToken, document.id());

    assertSuccessfulAuditResponse(response);

    List<Map<String, Object>> events = auditEvents(response);
    assertThat(events).hasSize(2);
    assertActions(events, "DOCUMENT_SUBMITTED", "DOCUMENT_REJECTED");

    Map<String, Object> rejectionEvent = events.get(1);
    assertAuditEvent(
        rejectionEvent,
        document.id(),
        "DOCUMENT_REJECTED",
        "IN_REVIEW",
        "DRAFT",
        rejectionCorrelationId);

    assertThat(rejectionEvent.get("actorId"))
        .as("rejection actor must not be the document owner")
        .isNotEqualTo(document.createdBy().toString());
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Complete lifecycle produces an ordered append-only audit history")
  void completeLifecycleShouldProduceOrderedAuditHistory() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    String administratorToken = authentication.accessTokenFor(SeededUser.ADMIN);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Lifecycle audit"), "Audit trail validation");

    Response submission = documentsApi.submit(ownerToken, document.id());
    String submissionCorrelationId = successfulTransitionCorrelationId(submission);

    Response approval = documentsApi.approve(managerToken, document.id());
    String approvalCorrelationId = successfulTransitionCorrelationId(approval);

    Response archive = documentsApi.archive(administratorToken, document.id());
    String archiveCorrelationId = successfulTransitionCorrelationId(archive);

    Response response = auditApi.getWithToken(managerToken, document.id());

    assertSuccessfulAuditResponse(response);

    List<Map<String, Object>> events = auditEvents(response);

    assertThat(events).hasSize(3);
    assertActions(events, "DOCUMENT_SUBMITTED", "DOCUMENT_APPROVED", "DOCUMENT_ARCHIVED");
    assertOrderedByOccurredAt(events);

    assertAuditEvent(
        events.get(0),
        document.id(),
        "DOCUMENT_SUBMITTED",
        "DRAFT",
        "IN_REVIEW",
        submissionCorrelationId);

    assertAuditEvent(
        events.get(1),
        document.id(),
        "DOCUMENT_APPROVED",
        "IN_REVIEW",
        "APPROVED",
        approvalCorrelationId);

    assertAuditEvent(
        events.get(2),
        document.id(),
        "DOCUMENT_ARCHIVED",
        "APPROVED",
        "ARCHIVED",
        archiveCorrelationId);

    assertThat(events.get(2).get("actorId"))
        .as("archive actor must not be the document owner")
        .isNotEqualTo(document.createdBy().toString());
  }

  @ParameterizedTest(name = "{0} can read document audit history")
  @EnumSource(
      value = SeededUser.class,
      names = {"ADMIN", "MANAGER"})
  @Tag("requires-seed")
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Privileged roles can read audit history")
  void privilegedRoleShouldReadAuditHistory(SeededUser viewer) {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String viewerToken = authentication.accessTokenFor(viewer);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Privileged audit"), "Audit RBAC validation");

    documentFlow.submit(ownerToken, document.id());

    Response response = auditApi.getWithToken(viewerToken, document.id());

    assertSuccessfulAuditResponse(response);
    assertThat(auditEvents(response)).isNotEmpty();
  }

  @Test
  @Tag("requires-seed")
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Document owner cannot read audit history without audit permission")
  void collaboratorOwnerShouldReceiveForbiddenWhenReadingAuditHistory() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Forbidden audit"), "Audit RBAC validation");

    Response response = auditApi.getWithToken(ownerToken, document.id());

    assertThatApiError(response)
        .hasStatus(403)
        .hasMessage("Forbidden")
        .hasPath(auditPath(document.id()))
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("requires-seed")
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Private document is concealed from another collaborator")
  void anotherCollaboratorShouldReceiveNotFoundForPrivateDocumentAudit() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String otherCollaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_JOAO);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Concealed audit"), "Audit visibility validation");

    Response response = auditApi.getWithToken(otherCollaboratorToken, document.id());

    assertThatApiError(response)
        .hasStatus(404)
        .hasMessage("Document not found")
        .hasPath(auditPath(document.id()))
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("security")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Audit endpoint rejects requests without authentication")
  void requestWithoutAuthenticationShouldBeRejected() {
    UUID documentId = UUID.randomUUID();

    Response response = auditApi.getWithoutAuthentication(documentId);

    assertThatApiError(response)
        .hasStatus(401)
        .hasPath(auditPath(documentId))
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Unknown document audit history returns not found")
  void unknownDocumentShouldReturnNotFound() {
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    UUID unknownDocumentId = UUID.randomUUID();

    Response response = auditApi.getWithToken(managerToken, unknownDocumentId);

    assertThatApiError(response)
        .hasStatus(404)
        .hasMessage("Document not found")
        .hasPath(auditPath(unknownDocumentId))
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Rejected workflow transition does not create an audit event")
  void invalidTransitionShouldNotCreateAuditEvent() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Invalid transition audit"), "Audit integrity validation");

    Response before = auditApi.getWithToken(managerToken, document.id());
    assertSuccessfulAuditResponse(before);
    assertThat(auditEvents(before)).isEmpty();

    Response invalidApproval = documentsApi.approve(managerToken, document.id());

    assertThatApiError(invalidApproval)
        .hasStatus(409)
        .hasPath(workflowPath(document.id(), "approve"))
        .hasConsistentCorrelationId();

    Response after = auditApi.getWithToken(managerToken, document.id());

    assertSuccessfulAuditResponse(after);
    assertThat(auditEvents(after)).as("audit history after rejected transition").isEmpty();
  }

  @Test
  @Tag("requires-seed")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Existing audit events remain unchanged when new events are appended")
  void existingAuditEventsShouldRemainUnchangedWhenNewEventsAreAppended() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Append only audit"), "Audit immutability validation");

    Response submission = documentsApi.submit(ownerToken, document.id());
    successfulTransitionCorrelationId(submission);

    Response firstRead = auditApi.getWithToken(managerToken, document.id());
    assertSuccessfulAuditResponse(firstRead);

    List<Map<String, Object>> firstSnapshot = auditEvents(firstRead);
    assertThat(firstSnapshot).hasSize(1);

    Map<String, Object> submittedEventSnapshot = Map.copyOf(firstSnapshot.getFirst());

    Response approval = documentsApi.approve(managerToken, document.id());
    successfulTransitionCorrelationId(approval);

    Response secondRead = auditApi.getWithToken(managerToken, document.id());
    assertSuccessfulAuditResponse(secondRead);

    List<Map<String, Object>> secondSnapshot = auditEvents(secondRead);

    assertThat(secondSnapshot).hasSize(2);
    assertThat(secondSnapshot.getFirst())
        .as("previous audit event must remain unchanged after a new event is appended")
        .isEqualTo(submittedEventSnapshot);

    assertActions(secondSnapshot, "DOCUMENT_SUBMITTED", "DOCUMENT_APPROVED");
  }

  private void assertSuccessfulAuditResponse(Response response) {
    response.then().statusCode(200).body(matchesJsonSchemaInClasspath(AUDIT_SCHEMA));
    assertValidCorrelationId(response);
  }

  private String successfulTransitionCorrelationId(Response response) {
    assertThat(response.statusCode()).as("workflow transition status").isEqualTo(200);
    return assertValidCorrelationId(response);
  }

  private List<Map<String, Object>> auditEvents(Response response) {
    return response.jsonPath().getList("$");
  }

  private void assertAuditEvent(
      Map<String, Object> event,
      UUID expectedDocumentId,
      String expectedAction,
      String expectedPreviousStatus,
      String expectedNewStatus,
      String expectedCorrelationId) {

    assertUuid(event.get("id"), "audit event id");
    assertUuid(event.get("actorId"), "audit actor id");

    assertThat(event.get("documentId"))
        .as("audit document id")
        .isEqualTo(expectedDocumentId.toString());

    assertThat(event.get("action")).as("audit action").isEqualTo(expectedAction);

    assertThat(event.get("previousStatus"))
        .as("previous document status")
        .isEqualTo(expectedPreviousStatus);

    assertThat(event.get("newStatus")).as("new document status").isEqualTo(expectedNewStatus);

    assertThat(event.get("correlationId"))
        .as("audit correlation ID")
        .isEqualTo(expectedCorrelationId);

    assertTimestamp(event.get("occurredAt"));
  }

  private void assertActions(List<Map<String, Object>> events, String... expectedActions) {

    assertThat(events)
        .extracting(event -> event.get("action"))
        .containsExactly((Object[]) expectedActions);
  }

  private void assertOrderedByOccurredAt(List<Map<String, Object>> events) {
    for (int index = 1; index < events.size(); index++) {
      Instant previous = timestamp(events.get(index - 1));
      Instant current = timestamp(events.get(index));

      assertThat(current.compareTo(previous))
          .as("audit event %d must not occur before the previous event", index)
          .isGreaterThanOrEqualTo(0);
    }
  }

  private void assertUuid(Object value, String description) {
    assertThat(value).as(description).isInstanceOf(String.class);

    String uuid = (String) value;

    assertThat(uuid).as(description).isNotBlank();
    assertThatCodeIsValidUuid(uuid, description);
  }

  private void assertThatCodeIsValidUuid(String value, String description) {
    try {
      UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new AssertionError(
          description + " must be a valid UUID, but was <" + value + ">", exception);
    }
  }

  private void assertTimestamp(Object value) {
    assertThat(value).as("audit occurredAt").isInstanceOf(String.class);

    String timestamp = (String) value;

    assertThat(timestamp).as("audit occurredAt").isNotBlank();

    try {
      Instant.parse(timestamp);
    } catch (RuntimeException exception) {
      throw new AssertionError(
          "audit occurredAt must be a valid ISO-8601 instant, but was <" + timestamp + ">",
          exception);
    }
  }

  private Instant timestamp(Map<String, Object> event) {
    return Instant.parse((String) event.get("occurredAt"));
  }

  private String auditPath(UUID documentId) {
    return "/api/v1/documents/" + documentId + "/audit";
  }

  private String workflowPath(UUID documentId, String action) {
    return "/api/v1/documents/" + documentId + "/" + action;
  }

  private String uniqueTitle(String prefix) {
    return prefix + " " + UUID.randomUUID();
  }
}
