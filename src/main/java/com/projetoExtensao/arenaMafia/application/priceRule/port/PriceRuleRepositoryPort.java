package com.projetoExtensao.arenaMafia.application.priceRule.port;

import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.PriceRuleEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public interface PriceRuleRepositoryPort {
  PriceRule save(PriceRule priceRule);

  PriceRule findByIdOrElseThrow(UUID id);

  List<PriceRule> findAll(Specification<PriceRuleEntity> spec);

  PriceRule findDefaultRuleOrElseThrow();

  Optional<PriceRule> findByName(String name);
}
