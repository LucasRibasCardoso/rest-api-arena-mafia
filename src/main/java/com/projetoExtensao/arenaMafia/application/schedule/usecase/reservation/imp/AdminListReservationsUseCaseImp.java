package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.AdminListReservationsUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDateRangeException;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.ReservationSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request.AdminReservationSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminListReservationsUseCaseImp implements AdminListReservationsUseCase {

  private final ReservationRepositoryPort reservationRepositoryPort;
  private final ScheduleEntryEnrichmentService enrichmentService;

  public AdminListReservationsUseCaseImp(
      ReservationRepositoryPort reservationRepositoryPort,
      ScheduleEntryEnrichmentService enrichmentService) {
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.enrichmentService = enrichmentService;
  }

  @Override
  public Page<ReservationDetail> execute(
      AdminReservationSearchRequestDto criteria, Pageable pageable) {
    if (pageable.getSort().isUnsorted()) {
      pageable =
          PageRequest.of(
              pageable.getPageNumber(),
              pageable.getPageSize(),
              Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    validateSearchCriteria(criteria);
    Specification<ReservationEntity> spec = buildSpecification(criteria);
    var reservationsPage = reservationRepositoryPort.search(spec, pageable);

    return reservationsPage.map(enrichmentService::enrichReservation);
  }

  private Specification<ReservationEntity> buildSpecification(
      AdminReservationSearchRequestDto criteria) {
    Specification<ReservationEntity> specification = Specification.unrestricted();

    if (criteria.searchTerm() != null && !criteria.searchTerm().isEmpty()) {
      specification = specification.and(ReservationSpecification.byTerm(criteria.searchTerm()));
    }

    if (criteria.userId() != null) {
      specification = specification.and(ReservationSpecification.byUserId(criteria.userId()));
    }

    if (criteria.status() != null) {
      specification = specification.and(ReservationSpecification.byStatus(criteria.status()));
    }

    if (criteria.startDate() != null || criteria.endDate() != null) {
      specification =
          specification.and(
              ReservationSpecification.byDateRange(criteria.startDate(), criteria.endDate()));
    }

    return specification;
  }

  private void validateSearchCriteria(AdminReservationSearchRequestDto criteria) {
    if (criteria.startDate() != null
        && criteria.endDate() != null
        && criteria.startDate().isAfter(criteria.endDate())) {
      throw new InvalidDateRangeException();
    }
  }
}
