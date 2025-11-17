package com.projetoExtensao.arenaMafia.application.priceRule.usecase;

import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import java.util.UUID;

public interface FindPriceRuleByIdUseCase {
  PriceRule execute(UUID id);
}
