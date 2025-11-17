package com.projetoExtensao.arenaMafia.infrastructure.persistence.specification;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.PriceRuleEntity;
import org.springframework.data.jpa.domain.Specification;

public class PriceRuleSpecification {

  public static Specification<PriceRuleEntity> byActiveStatus(Boolean isActive) {
    if (isActive == null) {
      return null;
    }

    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isActive"), isActive);
  }
}
