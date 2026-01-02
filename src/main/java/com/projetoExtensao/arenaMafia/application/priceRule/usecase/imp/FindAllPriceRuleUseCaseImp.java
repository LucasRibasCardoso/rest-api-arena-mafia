package com.projetoExtensao.arenaMafia.application.priceRule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.usecase.FindAllPriceRuleUseCase;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FindAllPriceRuleUseCaseImp implements FindAllPriceRuleUseCase {

  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public FindAllPriceRuleUseCaseImp(PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  @Override
  public List<PriceRule> execute(Boolean isActive) {
    var specification = PriceRuleSpecification.byActiveStatus(isActive);
    return priceRuleRepositoryPort.findAll(specification);
  }
}
