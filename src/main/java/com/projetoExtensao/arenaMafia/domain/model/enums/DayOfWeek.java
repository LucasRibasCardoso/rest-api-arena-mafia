package com.projetoExtensao.arenaMafia.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDayNumberException;

public enum DayOfWeek {
  MONDAY(1),
  TUESDAY(2),
  WEDNESDAY(3),
  THURSDAY(4),
  FRIDAY(5),
  SATURDAY(6),
  SUNDAY(7);

  private final int dayNumber;

  DayOfWeek(int dayNumber) {
    this.dayNumber = dayNumber;
  }

  @JsonValue
  public int getDayNumber() {
    return dayNumber;
  }

  public String getDayName() {
    return this.name();
  }

  /**
   * Converte um número do dia (1-7) para o enum correspondente.
   *
   * @param dayNumber Número do dia (1=Segunda, 7=Domingo)
   * @return O enum correspondente
   * @throws InvalidDayNumberException se o número não for válido
   */
  public static DayOfWeek fromDayNumber(int dayNumber) {

    for (DayOfWeek day : DayOfWeek.values()) {
      if (day.dayNumber == dayNumber) {
        return day;
      }
    }
    throw new InvalidDayNumberException();
  }
}
