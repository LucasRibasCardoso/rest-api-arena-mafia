package com.projetoExtensao.arenaMafia.application.priceRule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.priceRule.ports.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.usecase.UpdateDefaultPriceRuleUseCase;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request.UpdateDefaultPriceRuleRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateDefaultPriceRuleUseCaseImp implements UpdateDefaultPriceRuleUseCase {

  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public UpdateDefaultPriceRuleUseCaseImp(PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  @Override
  public PriceRule execute(UpdateDefaultPriceRuleRequestDto request) {
    PriceRule defaultPriceRule = priceRuleRepositoryPort.findDefaultRuleOrElseThrow();
    defaultPriceRule.updatePrice(request.price());
    return priceRuleRepositoryPort.save(defaultPriceRule);
  }
}
