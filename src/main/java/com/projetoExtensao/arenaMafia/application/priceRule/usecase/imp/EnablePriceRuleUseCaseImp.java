package com.projetoExtensao.arenaMafia.application.priceRule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.usecase.EnablePriceRuleUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.PriceRuleAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnablePriceRuleUseCaseImp implements EnablePriceRuleUseCase {

  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public EnablePriceRuleUseCaseImp(PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  @Override
  public void execute(UUID id) {
    PriceRule priceRuleToEnable = priceRuleRepositoryPort.findByIdOrElseThrow(id);
    priceRuleToEnable.enable();
    validateBusinessRules(priceRuleToEnable);
    priceRuleRepositoryPort.save(priceRuleToEnable);
  }

  private void validateBusinessRules(PriceRule priceRuleToEnable) {
    var filterActiveRules = PriceRuleSpecification.byActiveStatus(true);
    List<PriceRule> activeRules = priceRuleRepositoryPort.findAll(filterActiveRules);

    for (PriceRule existingRule : activeRules) {
      // Ignora a própria regra (que já foi ativada)
      if (existingRule.getId().equals(priceRuleToEnable.getId())) {
        continue;
      }

      // Valida nome duplicado
      if (existingRule.getName().equalsIgnoreCase(priceRuleToEnable.getName())) {
        throw new PriceRuleAlreadyExistsException();
      }

      priceRuleToEnable.validateOverlapWith(existingRule);
    }
  }
}
