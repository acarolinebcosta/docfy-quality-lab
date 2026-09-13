import { check, fail, group, sleep } from "k6";

import {
  getDocument,
  getDocumentAudit,
  listCategories,
  listDocumentFiles,
  listDocuments,
  login,
} from "./docfy-api.js";
import { runtimeConfig } from "./runtime-config.js";

function jsonOrNull(response) {
  try {
    return response.json();
  } catch (_error) {
    return null;
  }
}

function header(response, expectedName) {
  const match = Object.keys(response.headers).find(
    (name) => name.toLowerCase() === expectedName.toLowerCase(),
  );

  return match ? response.headers[match] : null;
}

function hasCorrelationId(response) {
  const correlationId = header(response, "X-Correlation-ID");
  return typeof correlationId === "string" && correlationId.length > 0;
}

export function prepareDocumentCatalog() {
  const config = runtimeConfig();

  if (!config.password) {
    fail("DOCFY_TEST_PASSWORD is required for the performance test setup");
  }

  const loginResponse = login(config);
  const loginBody = jsonOrNull(loginResponse);
  const authenticated = check(loginResponse, {
    "setup login returns 200": (response) => response.status === 200,
    "setup login returns a bearer token": () =>
      typeof loginBody?.accessToken === "string" && loginBody.accessToken.length > 0,
    "setup login is traceable": hasCorrelationId,
  });

  if (!authenticated) {
    fail(`Performance setup authentication failed with status ${loginResponse.status}`);
  }

  const documentsResponse = listDocuments(config, loginBody.accessToken, "documents_setup");
  const documentsBody = jsonOrNull(documentsResponse);
  const documentAvailable = check(documentsResponse, {
    "setup document catalogue returns 200": (response) => response.status === 200,
    "setup document catalogue contains a visible document": () =>
      Array.isArray(documentsBody?.items) && documentsBody.items.length > 0,
    "setup document catalogue is traceable": hasCorrelationId,
  });

  if (!documentAvailable) {
    fail(`Performance setup could not select a document; status ${documentsResponse.status}`);
  }

  return {
    documentId: documentsBody.items[0].id,
    token: loginBody.accessToken,
  };
}

export function browseDocumentCatalog(data) {
  const config = runtimeConfig();

  group("browse document catalogue", () => {
    const categoriesResponse = listCategories(config, data.token);
    const categoriesBody = jsonOrNull(categoriesResponse);

    check(categoriesResponse, {
      "categories returns 200": (response) => response.status === 200,
      "categories returns a non-empty catalogue": () =>
        Array.isArray(categoriesBody) && categoriesBody.length > 0,
      "categories is traceable": hasCorrelationId,
    });

    const documentsResponse = listDocuments(config, data.token);
    const documentsBody = jsonOrNull(documentsResponse);

    check(documentsResponse, {
      "documents returns 200": (response) => response.status === 200,
      "documents returns a page": () =>
        Array.isArray(documentsBody?.items) && Number.isInteger(documentsBody?.page),
      "documents is traceable": hasCorrelationId,
    });

    const detailResponse = getDocument(config, data.token, data.documentId);
    const detailBody = jsonOrNull(detailResponse);

    check(detailResponse, {
      "document detail returns 200": (response) => response.status === 200,
      "document detail keeps the selected id": () => detailBody?.id === data.documentId,
      "document detail is traceable": hasCorrelationId,
    });

    const auditResponse = getDocumentAudit(config, data.token, data.documentId);
    const auditBody = jsonOrNull(auditResponse);

    check(auditResponse, {
      "document audit returns 200": (response) => response.status === 200,
      "document audit returns an event collection": () => Array.isArray(auditBody),
      "document audit is traceable": hasCorrelationId,
    });

    const filesResponse = listDocumentFiles(config, data.token, data.documentId);
    const filesBody = jsonOrNull(filesResponse);

    check(filesResponse, {
      "document files returns 200": (response) => response.status === 200,
      "document files returns a metadata collection": () => Array.isArray(filesBody),
      "document files is traceable": hasCorrelationId,
    });
  });

  sleep(config.thinkTimeSeconds);
}
