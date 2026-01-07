package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDateTimeSlotException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DateTimeSlot(LocalDate date, TimeInterval timeInterval) {

  public DateTimeSlot {
    if (date == null) {
      throw new InvalidDateTimeSlotException(ErrorCode.DATE_TIME_SLOT_DATE_REQUIRED);
    }

    if (timeInterval == null) {
      throw new InvalidDateTimeSlotException(ErrorCode.DATE_TIME_SLOT_TIME_INTERVAL_REQUIRED);
    }
  }

  /**
   * Verifica se este slot de data/hora está em andamento.
   *
   * <p>Um slot é considerado em andamento quando:
   * <ul>
   *   <li>O momento atual é igual ou posterior ao início do slot</li>
   *   <li>O momento atual é anterior ao fim do slot</li>
   * </ul>
   *
   * <p>Considera intervalos que atravessam a meia-noite (ex: 22:00 - 02:00).
   *
   * @return true se o slot estiver em andamento, false caso contrário
   */
  public boolean isInProgress() {
    LocalDateTime now = LocalDateTime.now();

    LocalTime startTime = timeInterval.startTime();
    LocalTime endTime = timeInterval.endTime();

    LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
    LocalDateTime endDateTime = calculateEndDateTime(startTime, endTime);

    return !now.isBefore(startDateTime) && now.isBefore(endDateTime);
  }

  /**
   * Calcula o DateTime de término considerando intervalos que atravessam a meia-noite.
   */
  private LocalDateTime calculateEndDateTime(LocalTime startTime, LocalTime endTime) {
    if (endTime.isBefore(startTime) || endTime.equals(LocalTime.MIDNIGHT)) {
      return LocalDateTime.of(date.plusDays(1), endTime);
    }
    return LocalDateTime.of(date, endTime);
  }
}
