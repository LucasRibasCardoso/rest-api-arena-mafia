package com.projetoExtensao.arenaMafia.infrastructure.persistence.specification;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import org.springframework.data.jpa.domain.Specification;

public class ModalitySpecification {

  public static Specification<ModalityEntity> byActiveStatus(Boolean isActive) {
    if (isActive == null) {
      return null;
    }

    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isActive"), isActive);
  }
}
