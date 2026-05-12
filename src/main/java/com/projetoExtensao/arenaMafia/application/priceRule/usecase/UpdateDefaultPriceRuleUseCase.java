package com.projetoExtensao.arenaMafia.application.priceRule.usecase;

import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request.UpdateDefaultPriceRuleRequestDto;

public interface UpdateDefaultPriceRuleUseCase {
  PriceRule execute(UpdateDefaultPriceRuleRequestDto request);
}
