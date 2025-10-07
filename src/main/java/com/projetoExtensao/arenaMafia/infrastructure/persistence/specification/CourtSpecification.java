package com.projetoExtensao.arenaMafia.infrastructure.persistence.specification;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import org.springframework.data.jpa.domain.Specification;

public class CourtSpecification {

  public static Specification<CourtEntity> byActiveStatus(Boolean isActive) {
    return (root, query, criteriaBuilder) -> {
      if (isActive == null) {
        return null;
      }
      return criteriaBuilder.equal(root.get("isActive"), isActive);
    };
  }
}
