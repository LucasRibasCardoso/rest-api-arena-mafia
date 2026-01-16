package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateDefaultPriceRuleRequestDto(
    @NotNull(message = "PRICE_RULE_PRICE_REQUIRED")
        @PositiveOrZero(message = "PRICE_RULE_PRICE_INVALID")
        BigDecimal price) {}
