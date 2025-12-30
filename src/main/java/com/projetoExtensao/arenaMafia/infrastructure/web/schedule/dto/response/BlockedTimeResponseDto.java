package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response;

import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BlockedTimeResponseDto(
    UUID id,
    ScheduleEntryType type,
    UUID courtId,
    LocalDate date,
    TimeIntervalDto timeInterval,
    Instant createdAt,
    String description,
    boolean isFullDay)
    implements ScheduleEntryResponseDto {}
