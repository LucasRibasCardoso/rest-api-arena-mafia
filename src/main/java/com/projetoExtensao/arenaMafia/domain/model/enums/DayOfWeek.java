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

  public static DayOfWeek convertToDayOfWeek(LocalDate date) {
    java.time.DayOfWeek javaDayOfWeek = date.getDayOfWeek();
    return DayOfWeek.valueOf(javaDayOfWeek.name());
  }

  /**
   * Converte o DayOfWeek para o valor usado pela função SQL DAYOFWEEK.
   *
   * <p>SQL DAYOFWEEK retorna: 1=Sunday, 2=Monday, 3=Tuesday, ..., 7=Saturday
   *
   * @return valor inteiro correspondente ao dia da semana no SQL
   */
  public int getSqlDayOfWeekValue() {
    return switch (this) {
      case SUNDAY -> 1;
      case MONDAY -> 2;
      case TUESDAY -> 3;
      case WEDNESDAY -> 4;
      case THURSDAY -> 5;
      case FRIDAY -> 6;
      case SATURDAY -> 7;
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
