package com.projetoExtensao.arenaMafia.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDayOfWeekException;
import java.time.LocalDate;

public enum DayOfWeek {
  MONDAY,
  TUESDAY,
  WEDNESDAY,
  THURSDAY,
  FRIDAY,
  SATURDAY,
  SUNDAY;

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

  public static DayOfWeek convertToDayOfWeek(LocalDate date) {
    java.time.DayOfWeek javaDayOfWeek = date.getDayOfWeek();
    return DayOfWeek.valueOf(javaDayOfWeek.name());
  }

  @JsonValue
  public String getDayName() {
    return name();
  }

  /**
   * Converte o DayOfWeek para o valor usado pela função PostgreSQL EXTRACT(DOW).
   *
   * <p>PostgreSQL EXTRACT(DOW) retorna: 0=Sunday, 1=Monday, 2=Tuesday, ..., 6=Saturday
   *
   * @return valor inteiro correspondente ao dia da semana no PostgreSQL
   */
  public int getDayOfWeekValue() {
    return switch (this) {
      case SUNDAY -> 0;
      case MONDAY -> 1;
      case TUESDAY -> 2;
      case WEDNESDAY -> 3;
      case THURSDAY -> 4;
      case FRIDAY -> 5;
      case SATURDAY -> 6;
    };
  }

  /**
   * Retorna o próximo dia da semana. Se for domingo, retorna segunda-feira.
   * @return Próximo DayOfWeek
   */
  public DayOfWeek next() {
    int nextIndex = (this.ordinal() + 1) % values().length;
    return values()[nextIndex];
  }
}
