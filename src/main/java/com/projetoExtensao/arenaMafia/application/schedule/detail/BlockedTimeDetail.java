package com.projetoExtensao.arenaMafia.application.schedule.detail;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.time.LocalDate;
import java.util.UUID;

public record BlockedTimeDetail(
    UUID blockedTimeId,
    UUID courtId,
    String courtName,
    LocalDate date,
    TimeInterval timeInterval,
    String description,
    boolean isFullDay,
    UUID recurringBlockedTimeId)
    implements ScheduleDetail {}
