import http from "k6/http";

function requestParameters(config, operation, token, contentType) {
  const headers = { Accept: "application/json" };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  if (contentType) {
    headers["Content-Type"] = contentType;
  }

  return {
    headers,
    tags: {
      name: operation,
      operation,
    },
    timeout: config.requestTimeout,
  };
}

export function login(config) {
  return http.post(
    `${config.baseUrl}/api/v1/auth/login`,
    JSON.stringify({
      email: config.userEmail,
      password: config.password,
    }),
    requestParameters(config, "authentication_setup", null, "application/json"),
  );
}

export function listDocuments(config, token, operation = "documents_list") {
  return http.get(
    `${config.baseUrl}/api/v1/documents?page=0&size=20`,
    requestParameters(config, operation, token),
  );
}

export function listCategories(config, token) {
  return http.get(
    `${config.baseUrl}/api/v1/categories`,
    requestParameters(config, "categories_list", token),
  );
}

export function getDocument(config, token, documentId) {
  return http.get(
    `${config.baseUrl}/api/v1/documents/${encodeURIComponent(documentId)}`,
    requestParameters(config, "document_detail", token),
  );
}

export function getDocumentAudit(config, token, documentId) {
  return http.get(
    `${config.baseUrl}/api/v1/documents/${encodeURIComponent(documentId)}/audit`,
    requestParameters(config, "document_audit", token),
  );
}

export function listDocumentFiles(config, token, documentId) {
  return http.get(
    `${config.baseUrl}/api/v1/documents/${encodeURIComponent(documentId)}/files`,
    requestParameters(config, "document_files", token),
  );
}
