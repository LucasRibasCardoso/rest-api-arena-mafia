package com.projetoExtensao.arenaMafia.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RoleEnum {
  ROLE_USER("ROLE_USER"),
  ROLE_ADMIN("ROLE_ADMIN"),
  ROLE_MODERATOR("ROLE_MODERATOR"),
  ROLE_SYSTEM("ROLE_SYSTEM");

  private final String value;

  RoleEnum(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
