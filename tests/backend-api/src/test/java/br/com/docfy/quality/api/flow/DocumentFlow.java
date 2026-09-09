package br.com.docfy.quality.api.flow;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.docfy.quality.api.client.CategoriesApiClient;
import br.com.docfy.quality.api.client.DocumentsApiClient;
import br.com.docfy.quality.api.model.request.CreateDocumentRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;

public final class DocumentFlow {

  private final CategoriesApiClient categoriesApi = new CategoriesApiClient();
  private final DocumentsApiClient documentsApi = new DocumentsApiClient();

  @Step("Read available document categories")
  public List<UUID> categoryIds(String accessToken) {
    Response response = categoriesApi.listWithToken(accessToken);
    assertThat(response.statusCode()).as("category catalogue precondition").isEqualTo(200);

    List<String> categoryIds = response.jsonPath().getList("id", String.class);
    assertThat(categoryIds).as("seeded document categories").isNotEmpty();
    return categoryIds.stream().map(UUID::fromString).toList();
  }

  public UUID firstCategoryId(String accessToken) {
    return categoryIds(accessToken).getFirst();
  }

  @Step("Create draft document: {title}")
  public DocumentFixture createDraft(String accessToken, String title, String description) {
    return createDraft(accessToken, title, description, firstCategoryId(accessToken));
  }

  @Step("Create draft document in category {categoryId}: {title}")
  public DocumentFixture createDraft(
      String accessToken, String title, String description, UUID categoryId) {
    Response response =
        documentsApi.create(accessToken, new CreateDocumentRequest(title, description, categoryId));

    response
        .then()
        .statusCode(201)
        .body(matchesJsonSchemaInClasspath("schemas/document-response.schema.json"));

    return new DocumentFixture(
        UUID.fromString(response.jsonPath().getString("id")),
        response.jsonPath().getString("documentCode"),
        title,
        description,
        categoryId,
        UUID.fromString(response.jsonPath().getString("createdBy")));
  }

  @Step("Submit document {documentId} for review")
  public void submit(String accessToken, UUID documentId) {
    Response response = documentsApi.submit(accessToken, documentId);
    assertThat(response.statusCode()).as("document submission precondition").isEqualTo(200);
    assertThat(response.jsonPath().getString("status")).isEqualTo("IN_REVIEW");
  }

  @Step("Approve document {documentId}")
  public void approve(String accessToken, UUID documentId) {
    Response response = documentsApi.approve(accessToken, documentId);
    assertThat(response.statusCode()).as("document approval precondition").isEqualTo(200);
    assertThat(response.jsonPath().getString("status")).isEqualTo("APPROVED");
  }
}
