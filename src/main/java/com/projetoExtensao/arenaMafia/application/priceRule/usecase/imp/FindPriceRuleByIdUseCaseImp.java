package com.projetoExtensao.arenaMafia.application.priceRule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.priceRule.ports.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.usecase.FindPriceRuleByIdUseCase;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindPriceRuleByIdUseCaseImp implements FindPriceRuleByIdUseCase {

  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public FindPriceRuleByIdUseCaseImp(PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  @Override
  public PriceRule execute(UUID id) {
    return priceRuleRepositoryPort.findByIdOrElseThrow(id);
  }
}
