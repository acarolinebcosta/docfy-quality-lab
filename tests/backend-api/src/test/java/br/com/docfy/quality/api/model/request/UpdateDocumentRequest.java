package br.com.docfy.quality.api.model.request;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class UpdateDocumentRequest {

  private final Map<String, Object> fields;

  private UpdateDocumentRequest(Map<String, Object> fields) {
    this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonValue
  public Map<String, Object> fields() {
    return fields;
  }

  public static final class Builder {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    private Builder() {}

    public Builder title(String title) {
      fields.put("title", title);
      return this;
    }

    public Builder description(String description) {
      fields.put("description", description);
      return this;
    }

    public Builder categoryId(UUID categoryId) {
      fields.put("categoryId", categoryId);
      return this;
    }

    public UpdateDocumentRequest build() {
      return new UpdateDocumentRequest(fields);
    }
  }
}
