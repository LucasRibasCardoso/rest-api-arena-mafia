package com.projetoExtensao.arenaMafia.infrastructure.persistence.specification;

import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class ReservationSpecification {

  public static Specification<ReservationEntity> byTerm(String term) {
    return (root, query, criteriaBuilder) -> {
      if (!StringUtils.hasText(term)) {
        return null;
      }
      String searchTerm = "%" + term.toLowerCase() + "%";

      var userJoin = root.join("user", JoinType.LEFT);
      var userMatches =
          criteriaBuilder.or(
              criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("username")), searchTerm),
              criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("fullName")), searchTerm),
              criteriaBuilder.like(userJoin.get("phone"), searchTerm));

      // Use distinct to avoid duplicate results when joining
      if (query != null) {
        query.distinct(true);
      }

      return userMatches;
    };
  }

  public static Specification<ReservationEntity> byUserId(UUID userId) {
    return (root, query, criteriaBuilder) -> {
      if (userId == null) {
        return null;
      }
      return criteriaBuilder.equal(root.get("userId"), userId);
    };
  }

  public static Specification<ReservationEntity> byDateRange(
      LocalDate startDate, LocalDate endDate) {
    return (root, query, criteriaBuilder) -> {
      if (startDate != null && endDate != null) {
        return criteriaBuilder.between(root.get("dateTimeSlot").get("date"), startDate, endDate);
      }
      if (startDate != null) {
        return criteriaBuilder.greaterThanOrEqualTo(
            root.get("dateTimeSlot").get("date"), startDate);
      }
      if (endDate != null) {
        return criteriaBuilder.lessThanOrEqualTo(root.get("dateTimeSlot").get("date"), endDate);
      }
      return null;
    };
  }

  public static Specification<ReservationEntity> byStatus(ReservationStatus status) {
    return (root, query, criteriaBuilder) -> {
      if (status == null) {
        return null;
      }
      return criteriaBuilder.equal(root.get("status"), status);
    };
  }
}
