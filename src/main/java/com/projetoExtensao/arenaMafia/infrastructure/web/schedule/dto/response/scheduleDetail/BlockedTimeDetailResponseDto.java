package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail;

import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;

import java.time.LocalDate;
import java.util.UUID;

public record BlockedTimeDetailResponseDto(
    UUID blockedTimeId,
    UUID courtId,
    String courtName,
    LocalDate date,
    TimeIntervalDto timeInterval,
    String description,
    boolean isFullDay,
    UUID recurringBlockedTimeId)
    implements ScheduleDetailResponseDto {}
