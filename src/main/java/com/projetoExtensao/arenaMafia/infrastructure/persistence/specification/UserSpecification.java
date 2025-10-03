package com.projetoExtensao.arenaMafia.infrastructure.persistence.specification;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecification {

  public static Specification<UserEntity> byTerm(String term) {
    return (root, query, criteriaBuilder) -> {
      if (!StringUtils.hasText(term)) {
        return null;
      }
      String searchTerm = "%" + term.toLowerCase() + "%";
      return criteriaBuilder.or(
          criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), searchTerm),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), searchTerm),
          criteriaBuilder.like(root.get("phone"), searchTerm));
    };
  }

  public static Specification<UserEntity> byCreationDateRange(Instant startDate, Instant endDate) {
    return (root, query, criteriaBuilder) -> {
      if (startDate != null && endDate != null) {
        return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
      }
      if (startDate != null) {
        return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
      }
      if (endDate != null) {
        return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
      }
      return null;
    };
  }

  public static Specification<UserEntity> byAccountStatus(AccountStatus status) {
    return (root, query, criteriaBuilder) -> {
      if (status == null) {
        return null;
      }
      return criteriaBuilder.equal(root.get("status"), status);
    };
  }

  public static Specification<UserEntity> byRole(RoleEnum role) {
    return (root, query, criteriaBuilder) -> {
      if (role == null) {
        return null;
      }
      return criteriaBuilder.equal(root.get("role"), role);
    };
  }
}
