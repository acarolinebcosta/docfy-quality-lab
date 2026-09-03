package br.com.docfy.quality.api.test.document;

import static br.com.docfy.quality.api.assertion.ApiErrorAssertions.assertThatApiError;
import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
@Feature("Documents")
@Tag("documents")
class DocumentsApiTest {

  private final AuthenticationApiClient authenticationApi = new AuthenticationApiClient();

  private final CategoriesApiClient categoriesApi = new CategoriesApiClient();

  private final DocumentsApiClient documentsApi = new DocumentsApiClient();

  @Test
  @Tag("smoke")
  @Tag("requires-seed")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Authenticated collaborator creates a draft document")
  void shouldCreateDraftDocument() {
    String token = login("ana@docfy.local");
    UUID categoryId = firstCategoryId(token);

    String title = "QA document " + UUID.randomUUID();

    CreateDocumentRequest request =
        new CreateDocumentRequest(
            title, "Created by external REST Assured quality suite", categoryId);

    Response response = documentsApi.create(token, request);

    response
        .then()
        .statusCode(201)
        .body(matchesJsonSchemaInClasspath("schemas/document-response.schema.json"))
        .body("id", notNullValue())
        .body("title", equalTo(title))
        .body("status", equalTo("DRAFT"))
        .body("category.id", equalTo(categoryId.toString()));

    assertThat(response.header("Location"))
        .as("document Location header")
        .startsWith("/api/v1/documents/");

    assertThat(response.asString()).doesNotContain("password").doesNotContain("passwordHash");

    assertValidCorrelationId(response);
  }

  @Test
  @Tag("regression")
  @Tag("requires-seed")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Authenticated user can list visible documents")
  void shouldListVisibleDocuments() {
    String token = login("ana@docfy.local");

    Response response = documentsApi.listWithToken(token);

    response
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/document-page-response.schema.json"));

    assertValidCorrelationId(response);
  }

  @Test
  @Tag("regression")
  @Tag("requires-seed")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Created document can be read by its creator")
  void shouldReadCreatedDocument() {
    String token = login("ana@docfy.local");
    UUID categoryId = firstCategoryId(token);

    String title = "Readable document " + UUID.randomUUID();

    Response creation =
        documentsApi.create(
            token, new CreateDocumentRequest(title, "Document read scenario", categoryId));

    creation.then().statusCode(201);

    UUID documentId = UUID.fromString(creation.jsonPath().getString("id"));

    Response response = documentsApi.getById(token, documentId);

    response
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/document-response.schema.json"))
        .body("id", equalTo(documentId.toString()))
        .body("title", equalTo(title))
        .body("status", equalTo("DRAFT"));

    assertValidCorrelationId(response);
  }

  @Test
  @Tag("contract")
  @Tag("requires-seed")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Blank document title is rejected")
  void shouldRejectBlankTitle() {
    String token = login("ana@docfy.local");
    UUID categoryId = firstCategoryId(token);

    Response response =
        documentsApi.create(
            token, new CreateDocumentRequest("   ", "Invalid document", categoryId));

    assertThatApiError(response)
        .hasStatus(400)
        .hasPath("/api/v1/documents")
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("contract")
  @Tag("requires-seed")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Document title longer than 255 characters is rejected")
  void shouldRejectTitleLongerThan255Characters() {
    String token = login("ana@docfy.local");
    UUID categoryId = firstCategoryId(token);

    String title = "x".repeat(256);

    Response response =
        documentsApi.create(
            token, new CreateDocumentRequest(title, "Invalid document", categoryId));

    assertThatApiError(response)
        .hasStatus(400)
        .hasPath("/api/v1/documents")
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("regression")
  @Tag("requires-seed")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Unknown document returns the standard not found contract")
  void shouldReturn404ForUnknownDocument() {
    String token = login("ana@docfy.local");
    UUID unknownId = UUID.randomUUID();

    Response response = documentsApi.getById(token, unknownId);

    assertThatApiError(response)
        .hasStatus(404)
        .hasPath("/api/v1/documents/" + unknownId)
        .hasConsistentCorrelationId();
  }

  private String login(String email) {
    Optional<String> configuredPassword = TestConfiguration.testPassword();

    assumeTrue(
        configuredPassword.isPresent(), "Set DOCFY_TEST_PASSWORD to execute seeded user scenarios");

    Response login =
        authenticationApi.login(new LoginRequest(email, configuredPassword.orElseThrow()));

    login.then().statusCode(200);

    return login.jsonPath().getString("accessToken");
  }

  private UUID firstCategoryId(String token) {
    Response response = categoriesApi.listWithToken(token);

    response.then().statusCode(200);

    String categoryId = response.jsonPath().getString("[0].id");

    assertThat(categoryId).as("at least one seeded document category").isNotBlank();

    return UUID.fromString(categoryId);
  }
}
