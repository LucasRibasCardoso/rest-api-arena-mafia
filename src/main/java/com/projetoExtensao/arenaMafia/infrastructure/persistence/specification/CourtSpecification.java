package com.projetoExtensao.arenaMafia.infrastructure.persistence.specification;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class CourtSpecification {

  public static Specification<CourtEntity> byActiveStatus(Boolean isActive) {
    if (isActive == null) {
      return null;
    }

    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isActive"), isActive);
  }

  public static Specification<CourtEntity> byModalityId(UUID modalityId) {
    if (modalityId == null) {
      return null;
    }

    return (root, query, criteriaBuilder) ->
        criteriaBuilder.isMember(modalityId, root.get("modalityIds"));
  }
}
