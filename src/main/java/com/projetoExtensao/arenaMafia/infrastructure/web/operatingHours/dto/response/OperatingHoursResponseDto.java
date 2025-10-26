package com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record OperatingHoursResponseDto(
    UUID id, Set<String> daysOfWeek, TimeIntervalDto timeInterval, boolean isActive) {}
