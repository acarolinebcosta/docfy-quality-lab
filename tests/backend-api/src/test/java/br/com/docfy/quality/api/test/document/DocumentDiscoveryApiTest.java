package br.com.docfy.quality.api.test.document;

import static br.com.docfy.quality.api.assertion.ApiErrorAssertions.assertThatApiError;
import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Epic("Backend API")
@Feature("Document discovery")
@Tag("documents")
@Tag("regression")
@Tag("requires-seed")
class DocumentDiscoveryApiTest {

  private final AuthenticationFlow authentication = new AuthenticationFlow();
  private final DocumentFlow documentFlow = new DocumentFlow();
  private final DocumentsApiClient documentsApi = new DocumentsApiClient();

  @Test
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Search returns only documents visible to each role")
  void searchShouldRespectDocumentVisibility() {
    String collaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String otherCollaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_JOAO);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    String marker = uniqueMarker("Visibility");

    DocumentFixture ownDraft =
        documentFlow.createDraft(collaboratorToken, marker + " own", "Visible to owner");
    DocumentFixture privateDraft =
        documentFlow.createDraft(
            otherCollaboratorToken, marker + " private", "Hidden from collaborator");
    DocumentFixture approved =
        documentFlow.createDraft(
            otherCollaboratorToken, marker + " approved", "Visible to collaborator");
    documentFlow.submit(otherCollaboratorToken, approved.id());
    documentFlow.approve(managerToken, approved.id());

    Response collaboratorSearch =
        documentsApi.listWithToken(collaboratorToken, Map.of("search", marker, "size", 10));
    assertPageContract(collaboratorSearch);
    assertThat(documentIds(collaboratorSearch))
        .containsExactlyInAnyOrder(ownDraft.id(), approved.id())
        .doesNotContain(privateDraft.id());

    Response managerSearch =
        documentsApi.listWithToken(managerToken, Map.of("search", marker, "size", 10));
    assertPageContract(managerSearch);
    assertThat(documentIds(managerSearch))
        .containsExactlyInAnyOrder(ownDraft.id(), privateDraft.id(), approved.id());
  }

  @Test
  @Tag("security")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Visibility is applied before pagination")
  void visibilityShouldBeAppliedBeforePagination() {
    String collaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String otherCollaboratorToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_JOAO);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    String marker = uniqueMarker("Pagination");

    DocumentFixture ownDraft =
        documentFlow.createDraft(collaboratorToken, marker + " own", "Visible to owner");
    DocumentFixture hiddenDraft =
        documentFlow.createDraft(
            otherCollaboratorToken, marker + " hidden", "Must not affect pagination");
    DocumentFixture approved =
        documentFlow.createDraft(
            otherCollaboratorToken, marker + " approved", "Visible to collaborator");
    documentFlow.submit(otherCollaboratorToken, approved.id());
    documentFlow.approve(managerToken, approved.id());

    Response firstPage =
        documentsApi.listWithToken(
            collaboratorToken, Map.of("search", marker, "page", 0, "size", 1));

    assertPageContract(firstPage);
    assertThat(firstPage.jsonPath().getInt("page")).isZero();
    assertThat(firstPage.jsonPath().getInt("size")).isEqualTo(1);
    assertThat(firstPage.jsonPath().getLong("totalElements")).isEqualTo(2);
    assertThat(firstPage.jsonPath().getInt("totalPages")).isEqualTo(2);
    assertThat(documentIds(firstPage))
        .hasSize(1)
        .allMatch(id -> id.equals(ownDraft.id()) || id.equals(approved.id()))
        .doesNotContain(hiddenDraft.id());
  }

  @Test
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Search, category and status filters are combined")
  void listShouldCombineSearchCategoryAndStatusFilters() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    List<UUID> categories = documentFlow.categoryIds(ownerToken);
    assertThat(categories).as("at least two seeded categories").hasSizeGreaterThanOrEqualTo(2);
    UUID selectedCategory = categories.get(0);
    UUID otherCategory = categories.get(1);
    String marker = uniqueMarker("Combined filters");

    DocumentFixture expected =
        documentFlow.createDraft(
            ownerToken, marker + " expected", "Matches all filters", selectedCategory);
    documentFlow.submit(ownerToken, expected.id());
    documentFlow.approve(managerToken, expected.id());

    documentFlow.createDraft(
        ownerToken, marker + " wrong status", "Draft in selected category", selectedCategory);
    DocumentFixture wrongCategory =
        documentFlow.createDraft(
            ownerToken, marker + " wrong category", "Approved in another category", otherCategory);
    documentFlow.submit(ownerToken, wrongCategory.id());
    documentFlow.approve(managerToken, wrongCategory.id());

    Response response =
        documentsApi.listWithToken(
            managerToken,
            Map.of(
                "search",
                marker,
                "categoryId",
                selectedCategory,
                "status",
                "APPROVED",
                "size",
                10));

    assertPageContract(response);
    assertThat(response.jsonPath().getLong("totalElements")).isEqualTo(1);
    assertThat(documentIds(response)).containsExactly(expected.id());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidQueries")
  @Tag("contract")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Invalid list criteria return the standard error contract")
  void invalidListCriteriaShouldBeRejected(
      String scenario, Map<String, ?> query, String expectedMessage) {
    String token = authentication.accessTokenFor(SeededUser.ADMIN);

    Response response = documentsApi.listWithToken(token, query);

    assertThatApiError(response)
        .hasStatus(400)
        .hasMessage(expectedMessage)
        .hasPath("/api/v1/documents")
        .hasConsistentCorrelationId();
  }

  private static Stream<Arguments> invalidQueries() {
    return Stream.of(
        Arguments.of(
            "page size above the supported maximum",
            Map.of("size", 101),
            "Size must be between 1 and 100"),
        Arguments.of(
            "unknown document status",
            Map.of("status", "REJECTED"),
            "Invalid document status filter"));
  }

  private void assertPageContract(Response response) {
    response
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/document-page-response.schema.json"));
    assertValidCorrelationId(response);
  }

  private List<UUID> documentIds(Response response) {
    return response.jsonPath().getList("items.id", String.class).stream()
        .map(UUID::fromString)
        .toList();
  }

  private String uniqueMarker(String prefix) {
    return prefix + " " + UUID.randomUUID();
  }
}
