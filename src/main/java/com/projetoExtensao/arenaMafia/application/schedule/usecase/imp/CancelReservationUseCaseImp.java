package com.projetoExtensao.arenaMafia.application.schedule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationCancelledEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.CancelReservationUseCase;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidReservationException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class CancelReservationUseCaseImp implements CancelReservationUseCase {

  private static final long MINIUM_MINUTES_BEFORE_CANCELLATION_DEFAULT = 90;

  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final UserRepositoryPort userRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;

  public CancelReservationUseCaseImp(
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort,
      UserRepositoryPort userRepositoryPort,
      ApplicationEventPublisher eventPublisher) {
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.userRepositoryPort = userRepositoryPort;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void execute(UUID userId, UUID reservationId) {
    Reservation reservation = scheduleEntryRepositoryPort.findReservationByIdAndUserIdOrElseThrow(reservationId, userId);

    validateCancellationPolicy(reservation);

    reservation.cancel();
    Reservation cancelledReservation = (Reservation) scheduleEntryRepositoryPort.save(reservation);

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

    OnReservationCancelledEvent event =
        new OnReservationCancelledEvent(reservation, user.getUsername(), user.getPhone());

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
