package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateOperatingHoursRequestDto(
    @NotNull(message = "DAY_OF_WEEK_REQUIRED") DayOfWeek dayOfWeek,
    @NotNull(message = "TIME_INTERVAL_REQUIRED") @Valid TimeInterval timeInterval) {}
