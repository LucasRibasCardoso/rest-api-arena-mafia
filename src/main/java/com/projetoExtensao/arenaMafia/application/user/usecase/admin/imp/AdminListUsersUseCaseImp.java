package com.projetoExtensao.arenaMafia.application.user.usecase.admin.imp;

import com.projetoExtensao.arenaMafia.application.user.port.repository.AdminUserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.admin.AdminListUsersUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDateRangeException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.UserSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request.AdminUserSearchRequestDto;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminListUsersUseCaseImp implements AdminListUsersUseCase {

  public static final String ZONE_ID = "America/Sao_Paulo";
  private final AdminUserRepositoryPort adminUserRepository;

  public AdminListUsersUseCaseImp(AdminUserRepositoryPort adminUserRepository) {
    this.adminUserRepository = adminUserRepository;
  }

  @Override
  public Page<User> execute(AdminUserSearchRequestDto criteria, Pageable pageable) {
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(
              pageable.getPageNumber(),
              pageable.getPageSize(),
              Sort.by(Sort.Direction.DESC, "createdAt")
      );
    }

    validateSearchCriteria(criteria);
    Specification<UserEntity> spec = buildSpecification(criteria);
    return adminUserRepository.search(spec, pageable);
  }

  private Specification<UserEntity> buildSpecification(AdminUserSearchRequestDto criteria) {
    Specification<UserEntity> specification = Specification.unrestricted();

    if (criteria.term() != null && !criteria.term().isEmpty()) {
      specification = specification.and(UserSpecification.byTerm(criteria.term()));
    }

    if (criteria.status() != null) {
      specification = specification.and(UserSpecification.byAccountStatus(criteria.status()));
    }

    if (criteria.role() != null) {
      specification = specification.and(UserSpecification.byRole(criteria.role()));
    }

    if (criteria.createdAtStart() != null || criteria.createdAtEnd() != null) {
      Instant startInstant = null;
      if (criteria.createdAtStart() != null) {
        startInstant = criteria.createdAtStart().atStartOfDay(ZoneId.of(ZONE_ID)).toInstant();
      }

      Instant endInstant = null;
      if (criteria.createdAtEnd() != null) {
        endInstant =
            criteria.createdAtEnd().atTime(LocalTime.MAX).atZone(ZoneId.of(ZONE_ID)).toInstant();
      }

      specification =
          specification.and(UserSpecification.byCreationDateRange(startInstant, endInstant));
    }
    return specification;
  }

  private void validateSearchCriteria(AdminUserSearchRequestDto criteria) {
    if (criteria.createdAtStart() != null
        && criteria.createdAtEnd() != null
        && criteria.createdAtStart().isAfter(criteria.createdAtEnd())) {
      throw new InvalidDateRangeException();
    }
  }
}
