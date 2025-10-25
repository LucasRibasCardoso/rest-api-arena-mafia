package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdatePriceRuleRequestDto(
    @NotBlank(message = "PRICE_RULE_NAME_REQUIRED")
    String name,

    @NotNull(message = "PRICE_RULE_PRICE_REQUIRED")
    @PositiveOrZero(message = "PRICE_RULE_PRICE_INVALID")
    BigDecimal price) {}

