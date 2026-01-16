package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationCancelledByUserEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CancelReservationUseCase;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidReservationException;
import com.projetoExtensao.arenaMafia.domain.model.User;
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
  private final UserRepositoryPort userRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;

  public CancelReservationUseCaseImp(
      ReservationRepositoryPort reservationRepositoryPort,
      UserRepositoryPort userRepositoryPort,
      ApplicationEventPublisher eventPublisher) {
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.userRepositoryPort = userRepositoryPort;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void execute(UUID userId, UUID reservationId) {
    Reservation reservation =
        reservationRepositoryPort.findReservationByIdAndUserIdOrElseThrow(reservationId, userId);

    validateCancellationPolicy(reservation);

    reservation.cancel();
    Reservation cancelledReservation = reservationRepositoryPort.save(reservation);

    publishCancellationEvent(cancelledReservation, userId);
  }

  /**
   * Publica evento de cancelamento de reserva para processamento assíncrono de notificações.
   *
   * @param reservation a reserva cancelada
   * @param userId ID do usuário que cancelou a reserva
   */
  private void publishCancellationEvent(Reservation reservation, UUID userId) {
    User user = userRepositoryPort.findByIdOrElseThrow(userId);

    OnReservationCancelledByUserEvent event =
        new OnReservationCancelledByUserEvent(reservation, user.getUsername(), user.getPhone());

    eventPublisher.publishEvent(event);
  }

  /**
   * Valida se a reserva pode ser cancelada conforme a política de cancelamento. A reserva deve ser
   *
   * @param reservation a reserva a ser validada
   * @throws InvalidReservationException se não atender a política de cancelamento
   */
  private void validateCancellationPolicy(Reservation reservation) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime reservationDateTime =
        LocalDateTime.of(
            reservation.getDateTimeSlot().date(),
            reservation.getDateTimeSlot().timeInterval().startTime());

    long minutesUntilReservation = Duration.between(now, reservationDateTime).toMinutes();

    if (minutesUntilReservation < MINIUM_MINUTES_BEFORE_CANCELLATION_DEFAULT) {
      throw new InvalidReservationException(ErrorCode.RESERVATION_NOT_POSSIBLE_TO_CANCEL);
    }
  }
}
