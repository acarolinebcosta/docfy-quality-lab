package br.com.docfy.quality.api.client;

import static br.com.docfy.quality.api.specification.RequestSpecifications.authenticated;
import static br.com.docfy.quality.api.specification.RequestSpecifications.defaultRequest;
import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import java.util.UUID;

public final class DocumentAuditApiClient {

  public Response getWithoutAuthentication(UUID documentId) {
    return given().spec(defaultRequest()).when().get("/api/v1/documents/{id}/audit", documentId);
  }

  public Response getWithToken(String accessToken, UUID documentId) {
    return given()
        .spec(authenticated(accessToken))
        .when()
        .get("/api/v1/documents/{id}/audit", documentId);
  }
}
