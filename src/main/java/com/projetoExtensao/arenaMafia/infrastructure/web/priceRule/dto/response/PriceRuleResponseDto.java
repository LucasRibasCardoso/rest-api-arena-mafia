package com.projetoExtensao.arenaMafia.infrastructure.web.priceRule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PriceRuleResponseDto(
    UUID id,
    String name,
    Set<DayOfWeek> daysOfWeek,
    TimeIntervalDto timeInterval,
    BigDecimal price,
    int priority,
    boolean isDefault,
    boolean isActive) {}
