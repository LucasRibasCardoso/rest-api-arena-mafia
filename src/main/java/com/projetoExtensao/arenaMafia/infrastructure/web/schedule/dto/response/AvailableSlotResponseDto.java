package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailableSlotResponseDto(UUID courtId, TimeInterval timeInterval, BigDecimal price) {}
