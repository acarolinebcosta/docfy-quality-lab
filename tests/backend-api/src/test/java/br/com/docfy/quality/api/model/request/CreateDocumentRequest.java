package br.com.docfy.quality.api.model.request;

import java.util.UUID;

public record CreateDocumentRequest(String title, String description, UUID categoryId) {}
