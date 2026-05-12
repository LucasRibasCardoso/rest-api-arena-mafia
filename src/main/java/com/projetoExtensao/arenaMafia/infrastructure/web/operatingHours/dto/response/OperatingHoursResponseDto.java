package com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record OperatingHoursResponseDto(
    UUID id, Set<DayOfWeek> daysOfWeek, TimeIntervalDto timeInterval, boolean isActive) {}
