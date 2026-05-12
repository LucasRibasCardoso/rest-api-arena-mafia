package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record CreateOperatingHoursRequestDto(
    Set<DayOfWeek> daysOfWeek,
    @NotNull(message = "TIME_INTERVAL_REQUIRED") @Valid TimeInterval timeInterval) {}
