package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

import java.math.BigDecimal;
import java.util.UUID;

public record AvailableSlotResponseDto(UUID courtId, TimeInterval timeInterval, BigDecimal price) {}
