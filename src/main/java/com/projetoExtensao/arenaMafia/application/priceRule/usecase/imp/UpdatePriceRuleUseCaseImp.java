package com.projetoExtensao.arenaMafia.application.priceRule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.priceRule.ports.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.usecase.UpdatePriceRuleUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.PriceRuleAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.UpdatePriceRuleRequestDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdatePriceRuleUseCaseImp implements UpdatePriceRuleUseCase {

  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public UpdatePriceRuleUseCaseImp(PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  @Override
  public PriceRule execute(UUID ruleId, UpdatePriceRuleRequestDto request) {
    PriceRule priceRule = priceRuleRepositoryPort.findByIdOrElseThrow(ruleId);
    updatePriceRuleFields(priceRule, request);
    return priceRuleRepositoryPort.save(priceRule);
  }

  private void updatePriceRuleFields(PriceRule priceRule, UpdatePriceRuleRequestDto request) {
    if (request.name() != null && !request.name().equalsIgnoreCase(priceRule.getName())) {
      validateNameUniqueness(request.name(), priceRule.getId());
      priceRule.updateName(request.name());
    }

    priceRule.updatePrice(request.price());
  }

  private void validateNameUniqueness(String newName, UUID currentPriceRuleId) {
    priceRuleRepositoryPort
        .findByName(newName)
        .ifPresent(
            existingPriceRule -> {
              if (!existingPriceRule.getId().equals(currentPriceRuleId)) {
                if (existingPriceRule.isActive()) {
                  throw new PriceRuleAlreadyExistsException();
                }
              }
            });
  }
}
