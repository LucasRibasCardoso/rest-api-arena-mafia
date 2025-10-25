package com.projetoExtensao.arenaMafia.application.priceRule.usecase;

import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreatePriceRuleRequestDto;

public interface CreatePriceRuleUseCase {
  PriceRule execute(CreatePriceRuleRequestDto request);
}
