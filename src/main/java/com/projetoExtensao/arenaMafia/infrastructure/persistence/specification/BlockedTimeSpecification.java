package com.projetoExtensao.arenaMafia.infrastructure.persistence.specification;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.BlockedTimeEntity;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class BlockedTimeSpecification {

  private BlockedTimeSpecification() {}

  public static Specification<BlockedTimeEntity> byCourtId(UUID courtId) {
    return (root, query, criteriaBuilder) -> {
      Predicate forceJoin = criteriaBuilder.isNotNull(root.get("isFullDay"));

      if (courtId == null) {
        return forceJoin;
      }

      Predicate courtFilter = criteriaBuilder.equal(root.get("courtId"), courtId);

      return criteriaBuilder.and(courtFilter, forceJoin);
    };
  }
}
