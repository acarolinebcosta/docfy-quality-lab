package br.com.docfy.quality.api.test.document;

import static br.com.docfy.quality.api.assertion.ApiErrorAssertions.assertThatApiError;
import static br.com.docfy.quality.api.assertion.CorrelationIdAssertions.assertValidCorrelationId;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import br.com.docfy.quality.api.client.DocumentFilesApiClient;
import br.com.docfy.quality.api.flow.AuthenticationFlow;
import br.com.docfy.quality.api.flow.DocumentFixture;
import br.com.docfy.quality.api.flow.DocumentFlow;
import br.com.docfy.quality.api.model.SeededUser;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Epic("Backend API")
@Feature("Document files")
@Tag("files")
@Tag("regression")
@Tag("requires-seed")
class DocumentFilesApiTest {

  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String TEXT_CONTENT_TYPE = "text/plain";
  private static final int MAXIMUM_FILE_SIZE_BYTES = 10 * 1024 * 1024;

  private final AuthenticationFlow authentication = new AuthenticationFlow();
  private final DocumentFlow documentFlow = new DocumentFlow();
  private final DocumentFilesApiClient filesApi = new DocumentFilesApiClient();

  @Test
  @Tag("contract")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Owner uploads, lists and downloads a file with byte-for-byte integrity")
  void ownerShouldUploadListAndDownloadFile() throws IOException {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture document = createDraft(ownerToken, "File lifecycle");
    byte[] originalBytes = resourceBytes("/files/valid-document.pdf");

    Response upload =
        filesApi.upload(
            ownerToken, document.id(), "valid-document.pdf", PDF_CONTENT_TYPE, originalBytes);

    upload
        .then()
        .statusCode(201)
        .body(matchesJsonSchemaInClasspath("schemas/document-file-response.schema.json"))
        .body("id", notNullValue())
        .body("documentId", equalTo(document.id().toString()))
        .body("originalFilename", equalTo("valid-document.pdf"))
        .body("contentType", equalTo(PDF_CONTENT_TYPE))
        .body("size", equalTo(originalBytes.length))
        .body("uploadedBy", equalTo(document.createdBy().toString()));
    assertValidCorrelationId(upload);
    assertThat(Instant.parse(upload.jsonPath().getString("uploadedAt")))
        .isBeforeOrEqualTo(Instant.now());

    UUID fileId = UUID.fromString(upload.jsonPath().getString("id"));
    assertThat(upload.header("Location")).isEqualTo(filesPath(document.id()) + "/" + fileId);
    assertNoInternalMetadata(upload);

    Response list = filesApi.list(ownerToken, document.id());
    list.then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/document-file-list-response.schema.json"))
        .body("$", hasSize(1))
        .body("[0].id", equalTo(fileId.toString()))
        .body("[0].documentId", equalTo(document.id().toString()))
        .body("[0].originalFilename", equalTo("valid-document.pdf"));
    assertValidCorrelationId(list);
    assertNoInternalMetadata(list);

    Response download = filesApi.download(ownerToken, document.id(), fileId);
    download
        .then()
        .statusCode(200)
        .contentType(PDF_CONTENT_TYPE)
        .header("Content-Length", equalTo(Integer.toString(originalBytes.length)));
    assertThat(download.header("Content-Disposition"))
        .contains("attachment")
        .contains("valid-document.pdf");
    assertThat(download.asByteArray()).isEqualTo(originalBytes);
    assertValidCorrelationId(download);
  }

  @ParameterizedTest(name = "{0} uploads to a collaborator draft")
  @EnumSource(
      value = SeededUser.class,
      names = {"MANAGER", "ADMIN"})
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Privileged roles upload files to another user's draft")
  void privilegedRoleShouldUploadToCollaboratorDraft(SeededUser privilegedUser) {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String privilegedToken = authentication.accessTokenFor(privilegedUser);
    DocumentFixture document = createDraft(ownerToken, "Privileged file upload");
    String filename = privilegedUser.name().toLowerCase(Locale.ROOT) + "-evidence.txt";

    Response upload =
        filesApi.upload(
            privilegedToken,
            document.id(),
            filename,
            TEXT_CONTENT_TYPE,
            textBytes("Privileged evidence"));

    upload
        .then()
        .statusCode(201)
        .body(matchesJsonSchemaInClasspath("schemas/document-file-response.schema.json"))
        .body("documentId", equalTo(document.id().toString()))
        .body("originalFilename", equalTo(filename));
    assertValidCorrelationId(upload);

    filesApi
        .list(ownerToken, document.id())
        .then()
        .statusCode(200)
        .body("originalFilename", equalTo(List.of(filename)));
  }

  @Test
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Private draft file operations are concealed from another collaborator")
  void privateDraftFilesShouldBeConcealedFromAnotherCollaborator() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String otherToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_JOAO);
    DocumentFixture document = createDraft(ownerToken, "Private files");
    UUID existingFileId =
        uploadFixture(ownerToken, document.id(), "private.txt", textBytes("Private evidence"));

    Response upload =
        filesApi.upload(
            otherToken,
            document.id(),
            "intrusion.txt",
            TEXT_CONTENT_TYPE,
            textBytes("Unauthorized"));
    assertNotFoundDocument(upload, filesPath(document.id()));

    Response list = filesApi.list(otherToken, document.id());
    assertNotFoundDocument(list, filesPath(document.id()));

    Response download = filesApi.download(otherToken, document.id(), existingFileId);
    assertNotFoundDocument(download, filePath(document.id(), existingFileId));

    filesApi
        .list(ownerToken, document.id())
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].id", equalTo(existingFileId.toString()));
  }

  @Test
  @Tag("security")
  @Tag("rbac")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Collaborator reads another user's file after document approval")
  void approvedDocumentFileShouldBeVisibleToAnotherCollaborator() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String viewerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_JOAO);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document = createDraft(ownerToken, "Approved file visibility");
    byte[] content = textBytes("Approved evidence");
    UUID fileId = uploadFixture(ownerToken, document.id(), "approved.txt", content);
    documentFlow.submit(ownerToken, document.id());
    documentFlow.approve(managerToken, document.id());

    filesApi
        .list(viewerToken, document.id())
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].id", equalTo(fileId.toString()));

    Response download = filesApi.download(viewerToken, document.id(), fileId);
    download.then().statusCode(200);
    assertThat(download.asByteArray()).isEqualTo(content);
  }

  @Test
  @Tag("security")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Upload to an approved document is rejected without creating a file")
  void uploadToApprovedDocumentShouldBeRejectedWithoutSideEffect() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    String managerToken = authentication.accessTokenFor(SeededUser.MANAGER);
    DocumentFixture document = createDraft(ownerToken, "Approved upload restriction");
    UUID baselineFile =
        uploadFixture(ownerToken, document.id(), "baseline.txt", textBytes("Baseline"));
    documentFlow.submit(ownerToken, document.id());
    documentFlow.approve(managerToken, document.id());

    Response upload =
        filesApi.upload(
            managerToken, document.id(), "blocked.txt", TEXT_CONTENT_TYPE, textBytes("Blocked"));

    assertThatApiError(upload)
        .hasStatus(403)
        .hasMessage("Forbidden")
        .hasPath(filesPath(document.id()))
        .hasConsistentCorrelationId();
    filesApi
        .list(managerToken, document.id())
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].id", equalTo(baselineFile.toString()))
        .body("originalFilename", equalTo(List.of("baseline.txt")));
  }

  @Test
  @Tag("contract")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("Empty file is rejected without creating metadata")
  void emptyFileShouldBeRejectedWithoutSideEffect() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture document = createDraft(ownerToken, "Empty file validation");

    Response upload =
        filesApi.upload(ownerToken, document.id(), "empty.txt", TEXT_CONTENT_TYPE, new byte[0]);

    assertInvalidUpload(upload, document.id(), "File must not be empty");
    assertDocumentHasNoFiles(ownerToken, document.id());
  }

  @Test
  @Tag("contract")
  @Tag("security")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Declared PDF with incompatible signature is rejected")
  void incompatiblePdfSignatureShouldBeRejected() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture document = createDraft(ownerToken, "File signature validation");

    Response upload =
        filesApi.upload(
            ownerToken, document.id(), "fake.pdf", "application/pdf", textBytes("not-a-pdf"));

    assertInvalidUpload(upload, document.id(), "File content does not match its declared type");
    assertDocumentHasNoFiles(ownerToken, document.id());
  }

  @Test
  @Tag("contract")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("File immediately above the configured size limit is rejected")
  void fileAboveMaximumSizeShouldBeRejected() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture document = createDraft(ownerToken, "File size validation");
    byte[] oversizedContent = new byte[MAXIMUM_FILE_SIZE_BYTES + 1];
    Arrays.fill(oversizedContent, (byte) 'x');

    Response upload =
        filesApi.upload(
            ownerToken, document.id(), "oversized.txt", TEXT_CONTENT_TYPE, oversizedContent);

    assertInvalidUpload(upload, document.id(), "File exceeds the maximum allowed size");
    assertDocumentHasNoFiles(ownerToken, document.id());
  }

  @Test
  @Tag("contract")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Upload to an unknown document returns the standard safe error")
  void uploadToUnknownDocumentShouldReturnNotFound() {
    String administratorToken = authentication.accessTokenFor(SeededUser.ADMIN);
    UUID unknownDocumentId = UUID.randomUUID();

    Response upload =
        filesApi.upload(
            administratorToken,
            unknownDocumentId,
            "missing.txt",
            TEXT_CONTENT_TYPE,
            textBytes("Missing document"));

    assertNotFoundDocument(upload, filesPath(unknownDocumentId));
  }

  @Test
  @Tag("contract")
  @Severity(SeverityLevel.NORMAL)
  @DisplayName("Unknown file in a visible document returns the standard safe error")
  void unknownFileShouldReturnNotFound() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture document = createDraft(ownerToken, "Missing file");
    UUID unknownFileId = UUID.randomUUID();

    Response download = filesApi.download(ownerToken, document.id(), unknownFileId);

    assertThatApiError(download)
        .hasStatus(404)
        .hasMessage("File not found")
        .hasPath(filePath(document.id(), unknownFileId))
        .hasConsistentCorrelationId();
  }

  @Test
  @Tag("security")
  @Severity(SeverityLevel.BLOCKER)
  @DisplayName("Dangerous upload path is reduced to a safe basename")
  void dangerousFilenameShouldBeNormalizedToBasename() {
    String ownerToken = authentication.accessTokenFor(SeededUser.COLLABORATOR_ANA);
    DocumentFixture document = createDraft(ownerToken, "Filename safety");

    Response upload =
        filesApi.upload(
            ownerToken,
            document.id(),
            "../../quality-evidence.txt",
            TEXT_CONTENT_TYPE,
            textBytes("Safe filename"));

    upload.then().statusCode(201).body("originalFilename", equalTo("quality-evidence.txt"));
    UUID fileId = UUID.fromString(upload.jsonPath().getString("id"));

    Response download = filesApi.download(ownerToken, document.id(), fileId);
    download.then().statusCode(200);
    assertThat(download.header("Content-Disposition"))
        .contains("quality-evidence.txt")
        .doesNotContain("..")
        .doesNotContain("../");
  }

  private DocumentFixture createDraft(String ownerToken, String titlePrefix) {
    return documentFlow.createDraft(
        ownerToken, titlePrefix + " " + UUID.randomUUID(), "Document file risk validation");
  }

  private UUID uploadFixture(String token, UUID documentId, String filename, byte[] content) {
    Response upload = filesApi.upload(token, documentId, filename, TEXT_CONTENT_TYPE, content);
    upload.then().statusCode(201);
    return UUID.fromString(upload.jsonPath().getString("id"));
  }

  private void assertInvalidUpload(Response response, UUID documentId, String expectedMessage) {
    assertThatApiError(response)
        .hasStatus(400)
        .hasMessage(expectedMessage)
        .hasPath(filesPath(documentId))
        .hasConsistentCorrelationId();
  }

  private void assertNotFoundDocument(Response response, String expectedPath) {
    assertThatApiError(response)
        .hasStatus(404)
        .hasMessage("Document not found")
        .hasPath(expectedPath)
        .hasConsistentCorrelationId();
  }

  private void assertDocumentHasNoFiles(String token, UUID documentId) {
    filesApi.list(token, documentId).then().statusCode(200).body("$", hasSize(0));
  }

  private void assertNoInternalMetadata(Response response) {
    String body = response.asString();
    assertThat(body)
        .doesNotContain("storageKey")
        .doesNotContain("storagePath")
        .doesNotContain("storageLocation");
  }

  private byte[] resourceBytes(String resourcePath) throws IOException {
    try (InputStream resource = getClass().getResourceAsStream(resourcePath)) {
      assertThat(resource).as("classpath resource %s", resourcePath).isNotNull();
      return resource.readAllBytes();
    }
  }

  private byte[] textBytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private String filesPath(UUID documentId) {
    return "/api/v1/documents/" + documentId + "/files";
  }

  private String filePath(UUID documentId, UUID fileId) {
    return filesPath(documentId) + "/" + fileId;
  }
}
