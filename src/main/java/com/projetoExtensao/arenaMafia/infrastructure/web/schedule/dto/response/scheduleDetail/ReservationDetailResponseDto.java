package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail;

import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationDetailResponseDto(
    UUID reservationId,
    UUID userId,
    UUID courtId,
    String username,
    String userPhone,
    String courtName,
    LocalDate date,
    TimeIntervalDto timeInterval,
    String modalityName,
    BigDecimal price,
    ReservationStatus status,
    UUID recurringReservationId) implements ScheduleDetailResponseDto {}

