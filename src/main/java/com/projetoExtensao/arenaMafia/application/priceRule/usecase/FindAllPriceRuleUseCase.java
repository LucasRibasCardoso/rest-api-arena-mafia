package com.projetoExtensao.arenaMafia.application.priceRule.usecase;

import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import java.util.List;

public interface FindAllPriceRuleUseCase {
  List<PriceRule> execute(Boolean isActive);
}
