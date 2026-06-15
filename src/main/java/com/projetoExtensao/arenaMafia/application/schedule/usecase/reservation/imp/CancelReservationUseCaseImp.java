package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CancelReservationUseCase;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnReservationCancelledScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidReservationException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CancelReservationUseCaseImp implements CancelReservationUseCase {

  private static final long MINIUM_MINUTES_BEFORE_CANCELLATION_DEFAULT = 90;

  private final ReservationRepositoryPort reservationRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;

  public CancelReservationUseCaseImp(
      ReservationRepositoryPort reservationRepositoryPort,
      ApplicationEventPublisher eventPublisher) {
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void execute(UUID userId, UUID reservationId) {
    Reservation reservation =
        reservationRepositoryPort.findReservationByIdAndUserIdOrElseThrow(reservationId, userId);

    validateCancellationPolicy(reservation);
    reservation.cancel();
    reservationRepositoryPort.save(reservation);

    eventPublisher.publishEvent(new OnReservationCancelledScheduleTaskEvent(reservation.getId()));
  }

  /**
   * Valida se a reserva pode ser cancelada conforme a política de cancelamento. A reserva deve ser
   *
   * @param reservation a reserva a ser validada
   * @throws InvalidReservationException se não atender a política de cancelamento
   */
  private void validateCancellationPolicy(Reservation reservation) {
    LocalDateTime now = LocalDateTime.now();

    long minutesUntilReservation =
        Duration.between(now, reservation.getDateTimeSlot().getStartDateTime()).toMinutes();

    if (minutesUntilReservation < MINIUM_MINUTES_BEFORE_CANCELLATION_DEFAULT) {
      throw new InvalidReservationException(ErrorCode.RESERVATION_NOT_POSSIBLE_TO_CANCEL);
    }
  }
}
