package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleNormal;

import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationResponseDto(
    UUID id,
    ScheduleEntryType type,
    UUID courtId,
    LocalDate date,
    TimeIntervalDto timeInterval,
    Instant createdAt,
    UUID userId,
    UUID modalityId,
    UUID scheduledByAdminId,
    BigDecimal price,
    ReservationStatus status,
    UUID recurringReservationId)
    implements ScheduleEntryResponseDto {}
