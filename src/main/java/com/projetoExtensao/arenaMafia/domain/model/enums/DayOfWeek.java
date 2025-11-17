package com.projetoExtensao.arenaMafia.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDayOfWeekException;

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
      throw new InvalidDayOfWeekException(ErrorCode.DAY_OF_WEEK_REQUIRED);
    }
    try {
      return DayOfWeek.valueOf(value.toUpperCase().trim());
    } catch (IllegalArgumentException e) {
      throw new InvalidDayOfWeekException(ErrorCode.DAY_OF_WEEK_INVALID);
    }
  }
}
