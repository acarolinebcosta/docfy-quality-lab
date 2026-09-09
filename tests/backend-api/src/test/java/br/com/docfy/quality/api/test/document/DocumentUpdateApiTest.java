package br.com.docfy.quality.api.test.document;

import static br.com.docfy.quality.api.assertion.ApiErrorAssertions.assertThatApiError;
import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import br.com.docfy.quality.api.client.DocumentsApiClient;
import br.com.docfy.quality.api.flow.AuthenticationFlow;
import br.com.docfy.quality.api.flow.DocumentFixture;
import br.com.docfy.quality.api.flow.DocumentFlow;
import br.com.docfy.quality.api.model.SeededUser;
import br.com.docfy.quality.api.model.request.UpdateDocumentRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Epic("Backend API")
@Feature("Document update")
@Tag("documents")
@Tag("regression")
@Tag("requires-seed")
class DocumentUpdateApiTest {

  private final AuthenticationFlow authentication = new AuthenticationFlow();
  private final DocumentFlow documentFlow = new DocumentFlow();
  private final DocumentsApiClient documentsApi = new DocumentsApiClient();

  @Test
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Owner updates one field without changing omitted or immutable data")
  void ownerShouldUpdateOnlyTheProvidedField() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture original =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Partial update"), "Description must be preserved");
    String updatedTitle = uniqueTitle("Updated title");

    Response update =
        documentsApi.update(
            ownerToken, original.id(), UpdateDocumentRequest.builder().title(updatedTitle).build());

    update
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/document-response.schema.json"))
        .body("id", equalTo(original.id().toString()))
        .body("documentCode", equalTo(original.documentCode()))
        .body("title", equalTo(updatedTitle))
        .body("description", equalTo(original.description()))
        .body("category.id", equalTo(original.categoryId().toString()))
        .body("createdBy", equalTo(original.createdBy().toString()))
        .body("status", equalTo("DRAFT"));
    assertValidCorrelationId(update);

    Response persisted = documentsApi.getById(ownerToken, original.id());
    persisted
        .then()
        .statusCode(200)
        .body("title", equalTo(updatedTitle))
        .body("description", equalTo(original.description()));
  }

  @Test
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Explicit null clears the optional description")
  void ownerShouldClearDescriptionWhenNullIsExplicitlyProvided() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture original =
        documentFlow.createDraft(
            ownerToken, uniqueTitle("Clear description"), "Description to remove");

    Response update =
        documentsApi.update(
            ownerToken, original.id(), UpdateDocumentRequest.builder().description(null).build());

    update
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/document-response.schema.json"))
        .body("title", equalTo(original.title()))
        .body("description", nullValue());
    assertValidCorrelationId(update);

    Response persisted = documentsApi.getById(ownerToken, original.id());
    persisted.then().statusCode(200).body("description", nullValue());
  }

  @ParameterizedTest(name = "{0} updates a collaborator draft")
  @EnumSource(
      value = SeededUser.class,
      names = {"ADMIN", "MANAGER"})
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Privileged roles update a collaborator draft")
  void privilegedRoleShouldUpdateCollaboratorDraft(SeededUser editor) {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String editorToken = authentication.accessTokenFor(editor);
    DocumentFixture original =
        documentFlow.createDraft(ownerToken, uniqueTitle("Privileged update"), "Original value");
    String updatedTitle = uniqueTitle(editor + " update");

    Response update =
        documentsApi.update(
            editorToken,
            original.id(),
            UpdateDocumentRequest.builder().title(updatedTitle).build());

    update.then().statusCode(200).body("title", equalTo(updatedTitle));
    assertValidCorrelationId(update);

    documentsApi
        .getById(ownerToken, original.id())
        .then()
        .statusCode(200)
        .body("title", equalTo(updatedTitle));
  }

  @Test
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Collaborator cannot discover or update another collaborator draft")
  void collaboratorShouldNotUpdateAnotherCollaboratorDraft() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String otherToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_JOAO);
    DocumentFixture original =
        documentFlow.createDraft(ownerToken, uniqueTitle("Private draft"), "Protected value");

    Response update =
        documentsApi.update(
            otherToken,
            original.id(),
            UpdateDocumentRequest.builder().title("Unauthorized change").build());

    assertThatApiError(update)
        .hasStatus(404)
        .hasMessage("Document not found")
        .hasPath("/api/v1/documents/" + original.id())
        .hasConsistentCorrelationId();

    assertDocumentWasNotChanged(ownerToken, original);
  }

  @Test
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Approved document cannot be edited")
  void approvedDocumentShouldNotBeEdited() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture original =
        documentFlow.createDraft(ownerToken, uniqueTitle("Approved document"), "Protected value");
    documentFlow.submit(ownerToken, original.id());
    documentFlow.approve(managerToken, original.id());

    Response update =
        documentsApi.update(
            managerToken,
            original.id(),
            UpdateDocumentRequest.builder().title("Invalid state change").build());

    assertThatApiError(update)
        .hasStatus(403)
        .hasMessage("Forbidden")
        .hasPath("/api/v1/documents/" + original.id())
        .hasConsistentCorrelationId();

    Response persisted = documentsApi.getById(managerToken, original.id());
    persisted
        .then()
        .statusCode(200)
        .body("title", equalTo(original.title()))
        .body("status", equalTo("APPROVED"));
  }

  @Test
  @Tag("contract")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Empty update is rejected without changing the document")
  void emptyUpdateShouldBeRejected() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture original =
        documentFlow.createDraft(ownerToken, uniqueTitle("Empty update"), "Original value");

    Response update =
        documentsApi.update(ownerToken, original.id(), UpdateDocumentRequest.builder().build());

    assertThatApiError(update)
        .hasStatus(400)
        .hasMessage("At least one field must be provided")
        .hasPath("/api/v1/documents/" + original.id())
        .hasConsistentCorrelationId();

    assertDocumentWasNotChanged(ownerToken, original);
  }

  @Test
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Unknown category is rejected without changing the document")
  void unknownCategoryShouldBeRejected() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture original =
        documentFlow.createDraft(ownerToken, uniqueTitle("Category integrity"), "Original value");

    Response update =
        documentsApi.update(
            ownerToken,
            original.id(),
            UpdateDocumentRequest.builder().categoryId(UUID.randomUUID()).build());

    assertThatApiError(update)
        .hasStatus(400)
        .hasMessage("Category not found")
        .hasPath("/api/v1/documents/" + original.id())
        .hasConsistentCorrelationId();

    assertDocumentWasNotChanged(ownerToken, original);
  }

  private void assertDocumentWasNotChanged(String ownerToken, DocumentFixture original) {
    Response persisted = documentsApi.getById(ownerToken, original.id());

    persisted
        .then()
        .statusCode(200)
        .body("title", equalTo(original.title()))
        .body("description", equalTo(original.description()))
        .body("category.id", equalTo(original.categoryId().toString()));
  }

  private String uniqueTitle(String prefix) {
    return prefix + " " + UUID.randomUUID();
  }
}
