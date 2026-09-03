package br.com.docfy.quality.api.client;

import static br.com.docfy.quality.api.specification.RequestSpecifications.defaultRequest;
import static io.restassured.RestAssured.given;

import br.com.docfy.quality.api.model.request.LoginRequest;
import io.restassured.response.Response;

public final class AuthenticationApiClient {

  public Response login(LoginRequest request) {
    return given().spec(defaultRequest()).body(request).when().post("/api/v1/auth/login");
  }
}
