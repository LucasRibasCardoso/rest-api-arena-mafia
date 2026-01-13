package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

public record CreatePriceRuleRequestDto(
    @NotBlank(message = "PRICE_RULE_NAME_REQUIRED")
        @Size(min = 1, max = 100, message = "PRICE_RULE_NAME_INVALID_LENGTH")
        String name,
    Set<DayOfWeek> daysOfWeek,
    @NotNull(message = "TIME_INTERVAL_REQUIRED") @Valid TimeInterval timeInterval,
    @NotNull(message = "PRICE_RULE_PRICE_REQUIRED")
        @PositiveOrZero(message = "PRICE_RULE_PRICE_INVALID")
        BigDecimal price,
    @NotNull(message = "PRICE_RULE_PRIORITY_REQUIRED")
        @PositiveOrZero(message = "PRICE_RULE_PRIORITY_INVALID")
        Integer priority) {}
