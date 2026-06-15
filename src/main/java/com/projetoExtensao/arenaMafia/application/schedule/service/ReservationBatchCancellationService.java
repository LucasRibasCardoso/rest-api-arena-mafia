package com.projetoExtensao.arenaMafia.application.schedule.service;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationsCancelledByAdminNotificationEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnReservationCancelledScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.BatchCancellationFailedException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationBatchCancellationService {

  private static final String ACCOUNT_DISABLED_REASON =
      "Sua conta foi desativada pelo administrador. Suas reservas foram canceladas automaticamente.";

  private final ReservationRepositoryPort reservationRepository;
  private final UserRepositoryPort userRepository;
  private final ApplicationEventPublisher eventPublisher;

  public ReservationBatchCancellationService(
      ReservationRepositoryPort reservationRepository,
      UserRepositoryPort userRepository,
      ApplicationEventPublisher eventPublisher) {
    this.reservationRepository = reservationRepository;
    this.userRepository = userRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Cancela reservas em lote por ação de admin e lança exception se qualquer uma falhar.
   *
   * <p>Garante atomicidade: ou todas as reservas são canceladas com sucesso, ou nenhuma é cancelada
   * (rollback). As notificações só são enviadas após confirmar que todas as operações foram
   * bem-sucedidas.
   *
   * @param reservations Lista de reservas a serem canceladas
   * @param reason Motivo do cancelamento
   * @param adminId ID do administrador responsável pelo cancelamento
   * @return Quantidade de reservas canceladas com sucesso
   * @throws BatchCancellationFailedException se qualquer reserva falhar ao ser cancelada
   */
  @Transactional
  public int cancelReservationsInBatchByAdmin(
      List<Reservation> reservations, String reason, UUID adminId) {
    if (reservations == null || reservations.isEmpty()) return 0;

    Map<UUID, User> usersMap = fetchUsersInBatch(reservations);

    try {
      cancelByAdminAndSaveReservations(reservations, adminId);
      publishCancellationEvents(reservations, usersMap, reason);
    } catch (Exception e) {
      throw new BatchCancellationFailedException();
    }
    return reservations.size();
  }

  /**
   * Cancela reservas em lote devido à desativação de conta pelo admin.
   *
   * <p>Utilizado quando um administrador desativa a conta de um usuário. Notifica o usuário sobre o
   * cancelamento de suas reservas, pois ele não está ciente da ação.
   *
   * @param reservations Lista de reservas a serem canceladas
   * @param user Usuário dono das reservas (para notificação)
   * @return Quantidade de reservas canceladas com sucesso
   * @throws BatchCancellationFailedException se qualquer reserva falhar ao ser cancelada
   */
  @Transactional
  public int cancelReservationsDueToAccountDisabled(List<Reservation> reservations, User user) {
    if (reservations == null || reservations.isEmpty()) return 0;

    try {
      cancelAndSaveReservations(reservations);
      publishAccountDisabledCancellationEvent(reservations, user);
    } catch (Exception e) {
      throw new BatchCancellationFailedException();
    }
    return reservations.size();
  }

  /**
   * Cancela reservas em lote de forma silenciosa (sem notificação).
   *
   * <p>Utilizado quando o próprio usuário desativa sua conta. Neste cenário, não faz sentido
   * notificar o usuário sobre o cancelamento, pois ele já está ciente da ação.
   *
   * @param reservations Lista de reservas a serem canceladas
   * @return Quantidade de reservas canceladas com sucesso
   * @throws BatchCancellationFailedException se qualquer reserva falhar ao ser cancelada
   */
  @Transactional
  public int cancelReservationsInBatchSilently(List<Reservation> reservations) {
    if (reservations == null || reservations.isEmpty()) return 0;

    try {
      cancelAndSaveReservations(reservations);
    } catch (Exception e) {
      throw new BatchCancellationFailedException();
    }
    return reservations.size();
  }

  private Map<UUID, User> fetchUsersInBatch(List<Reservation> reservations) {
    Set<UUID> userIds =
        reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());

    List<User> users = userRepository.findAllByIds(userIds);

    return users.stream()
        .collect(Collectors.toMap(User::getId, user -> user, (existing, replacement) -> existing));
  }

  private void cancelByAdminAndSaveReservations(List<Reservation> reservations, UUID adminId) {
    reservations.forEach(
        reservation -> {
          reservation.cancelByAdmin(adminId);
          eventPublisher.publishEvent(
              new OnReservationCancelledScheduleTaskEvent(reservation.getId()));
        });
    reservationRepository.saveAll(reservations);
  }

  private void cancelAndSaveReservations(List<Reservation> reservations) {
    reservations.forEach(
        reservation -> {
          reservation.cancel();
          eventPublisher.publishEvent(
              new OnReservationCancelledScheduleTaskEvent(reservation.getId()));
        });
    reservationRepository.saveAll(reservations);
  }

  private void publishCancellationEvents(
      List<Reservation> reservations, Map<UUID, User> usersMap, String reason) {
    Map<UUID, List<Reservation>> reservationsByUser =
        reservations.stream().collect(Collectors.groupingBy(Reservation::getUserId));

    reservationsByUser.forEach(
        (userId, userReservations) -> {
          User user = usersMap.get(userId);
          eventPublisher.publishEvent(
              new OnReservationsCancelledByAdminNotificationEvent(
                  user.getUsername(), user.getPhone(), reason, reservations));
        });
  }

  private void publishAccountDisabledCancellationEvent(List<Reservation> reservations, User user) {
    var event =
        new OnReservationsCancelledByAdminNotificationEvent(
            user.getUsername(), user.getPhone(), ACCOUNT_DISABLED_REASON, reservations);
    eventPublisher.publishEvent(event);
  }
}
