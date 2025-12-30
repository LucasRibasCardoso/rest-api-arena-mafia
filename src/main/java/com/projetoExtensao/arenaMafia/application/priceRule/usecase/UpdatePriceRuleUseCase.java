package com.projetoExtensao.arenaMafia.application.priceRule.usecase;

import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request.UpdatePriceRuleRequestDto;
import java.util.UUID;

public interface UpdatePriceRuleUseCase {
  PriceRule execute(UUID ruleId, UpdatePriceRuleRequestDto request);
}
