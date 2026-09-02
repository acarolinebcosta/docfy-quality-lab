package br.com.docfy.quality.api.client;

import static br.com.docfy.quality.api.specification.RequestSpecifications.authenticated;
import static br.com.docfy.quality.api.specification.RequestSpecifications.defaultRequest;
import static io.restassured.RestAssured.given;

import io.restassured.response.Response;

public final class DocumentsApiClient {

  public Response listWithoutAuthentication() {
    return given().spec(defaultRequest()).queryParam("size", 1).when().get("/api/v1/documents");
  }

  public Response listWithToken(String accessToken) {
    return given()
        .spec(authenticated(accessToken))
        .queryParam("size", 1)
        .when()
        .get("/api/v1/documents");
  }
}
