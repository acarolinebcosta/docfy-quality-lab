package br.com.docfy.quality.api.client;

import static br.com.docfy.quality.api.specification.RequestSpecifications.authenticated;
import static br.com.docfy.quality.api.specification.RequestSpecifications.defaultRequest;
import static io.restassured.RestAssured.given;

import br.com.docfy.quality.api.model.request.CreateDocumentRequest;
import io.restassured.response.Response;
import java.util.UUID;

public final class DocumentsApiClient {

  public Response listWithoutAuthentication() {
    return given().spec(defaultRequest()).queryParam("size", 10).when().get("/api/v1/documents");
  }

  public Response listWithToken(String accessToken) {
    return given()
        .spec(authenticated(accessToken))
        .queryParam("size", 10)
        .when()
        .get("/api/v1/documents");
  }

  public Response create(String accessToken, CreateDocumentRequest request) {
    return given().spec(authenticated(accessToken)).body(request).when().post("/api/v1/documents");
  }

  public Response getById(String accessToken, UUID documentId) {
    return given()
        .spec(authenticated(accessToken))
        .when()
        .get("/api/v1/documents/{id}", documentId);
  }

  public Response submit(String accessToken, UUID documentId) {
    return given()
        .spec(authenticated(accessToken))
        .when()
        .post("/api/v1/documents/{id}/submit", documentId);
  }

  public Response approve(String accessToken, UUID documentId) {
    return given()
        .spec(authenticated(accessToken))
        .when()
        .post("/api/v1/documents/{id}/approve", documentId);
  }

  public Response reject(String accessToken, UUID documentId) {
    return given()
        .spec(authenticated(accessToken))
        .when()
        .post("/api/v1/documents/{id}/reject", documentId);
  }

  public Response archive(String accessToken, UUID documentId) {
    return given()
        .spec(authenticated(accessToken))
        .when()
        .post("/api/v1/documents/{id}/archive", documentId);
  }
}
