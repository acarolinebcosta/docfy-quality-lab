package br.com.docfy.quality.api.model;

public enum SeededUser {
  ADMIN("admin@docfy.local"),
  MANAGER("manager@docfy.local"),
  COLLABORATOR_ANA("ana@docfy.local"),
  COLLABORATOR_JOAO("joao@docfy.local");

  private final String email;

  SeededUser(String email) {
    this.email = email;
  }

  public String email() {
    return email;
  }
}
