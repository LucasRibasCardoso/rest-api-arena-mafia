package com.projetoExtensao.arenaMafia.application.schedule.service;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationsCancelledByAdminEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
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
   * Cancela reservas em lote e lança exception se qualquer uma falhar.
   *
   * <p>Garante atomicidade: ou todas as reservas são canceladas com sucesso, ou nenhuma é cancelada
   * (rollback). As notificações só são enviadas após confirmar que todas as operações foram
   * bem-sucedidas.
   *
   * @param reservations Lista de reservas a serem canceladas
   * @param reason Motivo do cancelamento
   * @param adminId ID do administrador responsável pelo cancelamento dos bloqueios
   * @return Quantidade de reservas canceladas com sucesso
   * @throws BatchCancellationFailedException se qualquer reserva falhar ao ser cancelada
   */
  @Transactional
  public int cancelReservationsInBatchByAdmin(List<Reservation> reservations, String reason, UUID adminId) {
    if (reservations == null || reservations.isEmpty()) return 0;

    Map<UUID, User> usersMap = fetchUsersInBatch(reservations);

    try {
      cancelAndSaveReservations(reservations, adminId);
      publishCancellationEvents(reservations, usersMap, reason);
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

  private void cancelAndSaveReservations(List<Reservation> reservations, UUID adminId) {
    for (Reservation reservation : reservations) {
      reservation.cancelByAdmin(adminId);
      reservationRepository.save(reservation);
    }
  }

  private void publishCancellationEvents(List<Reservation> reservations, Map<UUID, User> usersMap, String reason) {
    Map<UUID, List<Reservation>> reservationsByUser =
        reservations.stream().collect(Collectors.groupingBy(Reservation::getUserId));

      reservationsByUser.forEach((userId, userReservations) -> {
        User user = usersMap.get(userId);
        createAndPublishCancellationEvent(user, reason, userReservations);
      });
    }

  private void createAndPublishCancellationEvent(User user, String reason, List<Reservation> reservations) {
    var event = new OnReservationsCancelledByAdminEvent(user.getUsername(), user.getPhone(), reason, reservations);
    eventPublisher.publishEvent(event);
  }
}
