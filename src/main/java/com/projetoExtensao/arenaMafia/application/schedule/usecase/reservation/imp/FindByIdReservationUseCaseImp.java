package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.FindByIdReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.ReservationAccessDeniedException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FindByIdReservationUseCaseImp implements FindByIdReservationUseCase {

  private final ReservationRepositoryPort reservationRepositoryPort;

  public FindByIdReservationUseCaseImp(ReservationRepositoryPort reservationRepositoryPort) {
    this.reservationRepositoryPort = reservationRepositoryPort;
  }

  @Override
  public ScheduleEntry execute(UUID userId, UUID reservationId) {
    ScheduleEntry scheduleEntry = reservationRepositoryPort.findByIdOrElseThrow(reservationId);
    validateReservationOwnership(scheduleEntry, userId);
    return scheduleEntry;
  }

  /**
   * Valida se a reserva pertence ao usuário autenticado.
   *
   * @param scheduleEntry entrada de agendamento
   * @param userId ID do usuário autenticado
   * @throws ReservationAccessDeniedException se a reserva não pertencer ao usuário
   */
  private void validateReservationOwnership(ScheduleEntry scheduleEntry, UUID userId) {
    if (scheduleEntry instanceof Reservation reservation) {
      if (!reservation.getUserId().equals(userId)) {
        throw new ReservationAccessDeniedException();
      }
    }
  }
}
