package com.projetoExtensao.arenaMafia.application.schedule.detail;

import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationDetail(
    UUID reservationId,
    UUID userId,
    UUID courtId,
    String username,
    String fullName,
    String userPhone,
    String courtName,
    LocalDate date,
    TimeInterval timeInterval,
    String modalityName,
    BigDecimal price,
    ReservationStatus status,
    UUID recurringReservationId)
    implements ScheduleEntryDetail {

  @Override
  public UUID id() {
    return reservationId;
  }

  /**
   * Verifica se a reserva está em andamento no momento atual.
   *
   * @return true se a reserva estiver em andamento, false caso contrário.
   */
  public boolean isInProgress() {
    var dateTimeSlot = new DateTimeSlot(date, timeInterval);
    return dateTimeSlot.isInProgress();
  }
}
