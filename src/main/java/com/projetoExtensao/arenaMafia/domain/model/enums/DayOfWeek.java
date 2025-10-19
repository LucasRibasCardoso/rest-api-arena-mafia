package com.projetoExtensao.arenaMafia.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDayNumberException;

public enum DayOfWeek {
  MONDAY,
  TUESDAY,
  WEDNESDAY,
  THURSDAY,
  FRIDAY,
  SATURDAY,
  SUNDAY;

  @JsonValue
  public String getDayName() {
    return name();
  }

  @JsonCreator
  public static DayOfWeek fromString(String value) {
    if (value == null || value.isBlank()) {
      throw new InvalidDayNumberException();
    }
    try {
      return DayOfWeek.valueOf(value.toUpperCase().trim());
    } catch (IllegalArgumentException e) {
      throw new InvalidDayNumberException();
    }
  }
}
