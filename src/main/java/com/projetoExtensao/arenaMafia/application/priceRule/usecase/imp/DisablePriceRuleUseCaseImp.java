package com.projetoExtensao.arenaMafia.application.priceRule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.usecase.DisablePriceRuleUseCase;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisablePriceRuleUseCaseImp implements DisablePriceRuleUseCase {

  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public DisablePriceRuleUseCaseImp(PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  @Override
  public void execute(UUID id) {
    PriceRule priceRule = priceRuleRepositoryPort.findByIdOrElseThrow(id);
    priceRule.disable();
    priceRuleRepositoryPort.save(priceRule);
  }
}
