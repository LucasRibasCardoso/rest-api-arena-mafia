package com.projetoExtensao.arenaMafia.infrastructure.persistence.specification;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import org.springframework.data.jpa.domain.Specification;

public class OperatingHoursSpecification {

  public static Specification<OperatingHoursEntity> byActiveStatus(Boolean isActive) {
    if (isActive == null) {
      return null;
    }

    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isActive"), isActive);
  }
}
