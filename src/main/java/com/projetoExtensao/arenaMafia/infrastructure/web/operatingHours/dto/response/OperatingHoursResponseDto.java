package com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record OperatingHoursResponseDto(
    UUID id, String dayOfWeek, TimeIntervalDto timeInterval, boolean isActive) {}
