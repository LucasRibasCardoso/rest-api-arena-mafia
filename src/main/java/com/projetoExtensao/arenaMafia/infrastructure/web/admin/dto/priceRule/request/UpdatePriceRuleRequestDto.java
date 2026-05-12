package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdatePriceRuleRequestDto(
    @Size(min = 1, max = 100, message = "PRICE_RULE_NAME_INVALID_LENGTH") String name,
    @PositiveOrZero(message = "PRICE_RULE_PRICE_INVALID") BigDecimal price) {}
