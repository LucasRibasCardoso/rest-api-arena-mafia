package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationCancelledByAdminNotificationEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CancelReservationByAdminUseCase;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnReservationCancelledScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CancelReservationByAdminUseCaseImp implements CancelReservationByAdminUseCase {

  private static final String REASON = "Um administrador cancelou sua reserva";

  private final UserRepositoryPort userRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ReservationRepositoryPort reservationRepository;
  private final ReservationBatchCancellationService reservationBatchCancellationService;

  public CancelReservationByAdminUseCaseImp(
      UserRepositoryPort userRepository,
      ApplicationEventPublisher eventPublisher,
      ReservationRepositoryPort reservationRepository,
      ReservationBatchCancellationService reservationBatchCancellationService) {
    this.userRepository = userRepository;
    this.eventPublisher = eventPublisher;
    this.reservationRepository = reservationRepository;
    this.reservationBatchCancellationService = reservationBatchCancellationService;
  }

  @Override
  public void execute(UUID adminId, UUID reservationId, boolean cancelAllRecurring) {
    Reservation reservation = reservationRepository.findByIdOrElseThrow(reservationId);
    User costumer = userRepository.findByIdOrElseThrow(reservation.getUserId());

    if (reservation.isRecurring() && cancelAllRecurring) {
      processCancelRecurringReservation(adminId, reservation.getRecurringReservationId());
    } else {
      processCancelSingleReservation(adminId, reservation, costumer.getUsername(), costumer.getPhone());
    }
  }

  private void processCancelRecurringReservation(UUID adminId, UUID recurringReservationId) {
    List<Reservation> reservations = reservationRepository.findAllFutureRecurringReservations(recurringReservationId);
    reservationBatchCancellationService.cancelReservationsInBatchByAdmin(reservations, REASON, adminId);
  }

  private void processCancelSingleReservation(UUID adminId, Reservation reservation, String username, String phone) {
    reservation.cancelByAdmin(adminId);
    reservationRepository.save(reservation);

    eventPublisher.publishEvent(new OnReservationCancelledByAdminNotificationEvent(username, phone, REASON, reservation));
    eventPublisher.publishEvent(new OnReservationCancelledScheduleTaskEvent(reservation.getId()));
  }
}
