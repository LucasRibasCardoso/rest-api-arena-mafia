package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
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

  public FindByIdReservationUseCaseImp(ReservationRepositoryPort reservationRepositoryPort) {
    this.reservationRepositoryPort = reservationRepositoryPort;
  }

  @Override
  public Reservation execute(UUID userId, UUID reservationId) {
    Reservation reservation = reservationRepositoryPort.findByIdOrElseThrow(reservationId);
    validateReservationOwnership(reservation, userId);
    return reservation;
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
