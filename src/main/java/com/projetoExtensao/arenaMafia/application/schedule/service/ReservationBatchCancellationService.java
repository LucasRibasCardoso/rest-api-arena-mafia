package com.projetoExtensao.arenaMafia.application.schedule.service;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationCancelledByAdminEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.BatchCancellationFailedException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
   * <p>Garante atomicidade: ou todas as reservas são canceladas com sucesso,
   * ou nenhuma é cancelada (rollback). As notificações só são enviadas após
   * confirmar que todas as operações foram bem-sucedidas.
   *
   * @param reservations Lista de reservas a serem canceladas
   * @param reason Motivo do cancelamento
   * @return Quantidade de reservas canceladas com sucesso
   * @throws BatchCancellationFailedException se qualquer reserva falhar ao ser cancelada
   */
  @Transactional
  public int cancelReservationsInBatch(List<Reservation> reservations, String reason) {
    if (reservations == null || reservations.isEmpty()) {
      return 0;
    }

    Map<UUID, User> usersMap = fetchUsersInBatch(reservations);
    List<OnReservationCancelledByAdminEvent> pendingEvents = new ArrayList<>();

    try {
      for (Reservation reservation : reservations) {
        reservation.cancel();
        reservationRepository.save(reservation);
        createCancellationEvent(reservation, usersMap, reason).ifPresent(pendingEvents::add);
      }
    }
    catch (Exception e) {
      throw new BatchCancellationFailedException();
    }

    pendingEvents.forEach(eventPublisher::publishEvent);
    return reservations.size();
  }

  /**
   * Busca todos os usuários relacionados às reservas em uma única query.
   *
   * @param reservations Lista de reservas
   * @return Map com userId como chave e User como valor
   */
  private Map<UUID, User> fetchUsersInBatch(List<Reservation> reservations) {
    Set<UUID> userIds =
        reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());

    List<User> users = userRepository.findAllByIds(userIds);

    return users.stream()
        .collect(Collectors.toMap(User::getId, user -> user, (existing, replacement) -> existing));
  }

  /**
   * Cria evento de cancelamento de reserva.
   *
   * @param reservation Reserva cancelada
   * @param usersMap Map de usuários
   * @param reason Motivo do cancelamento
   * @return Optional contendo o evento de cancelamento, ou vazio se usuário não encontrado
   */
  private Optional<OnReservationCancelledByAdminEvent> createCancellationEvent(
          Reservation reservation, Map<UUID, User> usersMap, String reason) {

    return Optional.ofNullable(usersMap.get(reservation.getUserId()))
        .map(user -> new OnReservationCancelledByAdminEvent(reservation, user.getUsername(), user.getPhone(), reason));
  }
}
