package com.projetoExtensao.arenaMafia.domain.dto;

import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationDetail(
    UUID reservationId,
    UUID courtId,
    String username,
    String userPhone,
    String courtName,
    LocalDate date,
    TimeInterval timeInterval,
    String modalityName,
    BigDecimal price,
    ReservationStatus status,
    UUID recurringReservationId)
    implements ScheduleDetail {}
