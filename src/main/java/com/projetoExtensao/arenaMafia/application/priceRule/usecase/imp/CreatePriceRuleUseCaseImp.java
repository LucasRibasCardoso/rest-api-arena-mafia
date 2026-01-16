package com.projetoExtensao.arenaMafia.application.priceRule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.usecase.CreatePriceRuleUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.PriceRuleAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request.CreatePriceRuleRequestDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreatePriceRuleUseCaseImp implements CreatePriceRuleUseCase {

  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public CreatePriceRuleUseCaseImp(PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  @Override
  public PriceRule execute(CreatePriceRuleRequestDto request) {
    PriceRule newPriceRule =
        PriceRule.create(
            request.name(),
            request.daysOfWeek(),
            request.timeInterval(),
            request.price(),
            request.priority());

    validateBusinessRules(newPriceRule);

    return priceRuleRepositoryPort.save(newPriceRule);
  }

  private void validateBusinessRules(PriceRule newPriceRule) {
    var filterActiveRules = PriceRuleSpecification.byActiveStatus(true);
    List<PriceRule> activeRules = priceRuleRepositoryPort.findAll(filterActiveRules);

    for (PriceRule existingRule : activeRules) {

      // Verifica se já existe uma regra com o mesmo nome
      if (existingRule.getName().equalsIgnoreCase(newPriceRule.getName())) {
        throw new PriceRuleAlreadyExistsException();
      }

      // Verifica se há conflito de regras já existentes
      newPriceRule.validateOverlapWith(existingRule);
    }
  }
}
