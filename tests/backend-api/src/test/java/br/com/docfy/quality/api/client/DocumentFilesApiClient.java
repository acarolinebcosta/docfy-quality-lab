package br.com.docfy.quality.api.client;

import static br.com.docfy.quality.api.specification.RequestSpecifications.authenticated;
import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import java.util.UUID;

public final class DocumentFilesApiClient {

  private static final String FILES_PATH = "/api/v1/documents/{documentId}/files";

  public Response upload(
      String accessToken, UUID documentId, String filename, String contentType, byte[] content) {
    return given()
        .spec(authenticated(accessToken))
        .multiPart("file", filename, content, contentType)
        .when()
        .post(FILES_PATH, documentId);
  }

  public Response list(String accessToken, UUID documentId) {
    return given().spec(authenticated(accessToken)).when().get(FILES_PATH, documentId);
  }

  public Response download(String accessToken, UUID documentId, UUID fileId) {
    return given()
        .spec(authenticated(accessToken))
        .accept("*/*")
        .when()
        .get(FILES_PATH + "/{fileId}", documentId, fileId);
  }
}
