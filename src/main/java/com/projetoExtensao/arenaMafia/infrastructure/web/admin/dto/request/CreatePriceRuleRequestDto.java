package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO para criação de regras de preço.
 *
 * @param name Nome da regra de preço
 * @param daysOfWeek Dias da semana aplicáveis. Se null, a regra se aplica a todos os dias da
 *     semana
 * @param timeInterval Intervalo de tempo aplicável. Se null, a regra se aplica ao dia inteiro
 *     (00:00 às 23:59)
 * @param price Preço definido pela regra
 * @param priority Prioridade da regra (maior = mais prioritária)
 */
public record CreatePriceRuleRequestDto(
    @NotBlank(message = "PRICE_RULE_NAME_REQUIRED")
    String name,

    Set<DayOfWeek> daysOfWeek,

    @Valid
    TimeInterval timeInterval,

    @NotNull(message = "PRICE_RULE_PRICE_REQUIRED")
    @PositiveOrZero(message = "PRICE_RULE_PRICE_INVALID")
    BigDecimal price,

    @NotNull(message = "PRICE_RULE_PRIORITY_REQUIRED")
    @PositiveOrZero(message = "PRICE_RULE_PRIORITY_INVALID")
    Integer priority) {}
