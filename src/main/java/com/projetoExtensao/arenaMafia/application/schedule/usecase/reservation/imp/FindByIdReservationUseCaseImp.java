package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.FindByIdReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.ReservationAccessDeniedException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindByIdReservationUseCaseImp implements FindByIdReservationUseCase {

  private final ReservationRepositoryPort reservationRepositoryPort;
  private final ScheduleEntryEnrichmentService scheduleEntryEnrichmentService;

  public FindByIdReservationUseCaseImp(
      ReservationRepositoryPort reservationRepositoryPort,
      ScheduleEntryEnrichmentService scheduleEntryEnrichmentService) {
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.scheduleEntryEnrichmentService = scheduleEntryEnrichmentService;
  }

  @Override
  public ReservationDetail execute(UUID userId, UUID reservationId) {
    Reservation reservation = reservationRepositoryPort.findByIdOrElseThrow(reservationId);
    validateReservationOwnership(reservation, userId);
    return scheduleEntryEnrichmentService.enrichReservation(reservation);
  }

  /**
   * Valida se a reserva pertence ao usuário autenticado.
   *
   * @param reservation reserva a ser validada
   * @param userId ID do usuário autenticado
   * @throws ReservationAccessDeniedException se a reserva não pertencer ao usuário
   */
  private void validateReservationOwnership(Reservation reservation, UUID userId) {
    if (!reservation.getUserId().equals(userId)) {
      throw new ReservationAccessDeniedException();
    }
  }
}
