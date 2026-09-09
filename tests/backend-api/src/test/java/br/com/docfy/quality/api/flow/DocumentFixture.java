package br.com.docfy.quality.api.flow;

import java.util.UUID;

public record DocumentFixture(
    UUID id,
    String documentCode,
    String title,
    String description,
    UUID categoryId,
    UUID createdBy) {}
