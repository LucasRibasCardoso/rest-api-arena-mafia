package com.projetoExtensao.arenaMafia.application.schedule.service;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationCancelledByAdminEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.result.BatchCancellationResult;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservationBatchCancellationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReservationBatchCancellationService.class);

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

  @Transactional
  public BatchCancellationResult cancelReservationsInBatch(List<Reservation> reservations, String reason) {

    if (reservations == null || reservations.isEmpty()) {
      return BatchCancellationResult.empty();
    }

    Map<UUID, User> usersMap = fetchUsersInBatch(reservations);

    // Contadores para estatísticas
    int successCount = 0;
    int failureCount = 0;
    List<UUID> failedReservationIds = new ArrayList<>();

    // Processa cada reserva
    for (Reservation reservation : reservations) {
      try {
        processReservationCancellation(reservation, usersMap, reason);
        successCount++;

      } catch (Exception e) {
        handleCancellationError(reservation, e);
        failureCount++;
        failedReservationIds.add(reservation.getId());
      }
    }

    return new BatchCancellationResult(reservations.size(), successCount, failureCount, failedReservationIds);
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
   * Processa o cancelamento de uma reserva individual.
   *
   * @param reservation Reserva a ser cancelada
   * @param usersMap Map de usuários para busca rápida
   * @param reason Motivo do cancelamento
   */
  private void processReservationCancellation(Reservation reservation, Map<UUID, User> usersMap, String reason) {
    reservation.cancel();
    reservationRepository.save(reservation);
    publishCancellationEvent(reservation, usersMap, reason);
    LOGGER.debug("Reserva {} cancelada com sucesso", reservation.getId());
  }

  /**
   * Publica evento de cancelamento de reserva para processamento assíncrono de notificações. O
   * processamento assíncrono é gerenciado pelos listeners de eventos com @Async.
   *
   * @param reservation Reserva cancelada
   * @param usersMap Map de usuários
   * @param reason Motivo do cancelamento
   */
  private void publishCancellationEvent(Reservation reservation, Map<UUID, User> usersMap, String reason) {

    User user = usersMap.get(reservation.getUserId());

    if (user == null) {
      LOGGER.warn(
          "Usuário {} não encontrado para a reserva {}. Notificação não será enviada.",
          reservation.getUserId(),
          reservation.getId());
      return;
    }

    eventPublisher.publishEvent(
        new OnReservationCancelledByAdminEvent(
            reservation, user.getUsername(), user.getPhone(), reason));
  }

  /**
   * Trata erros que ocorrem durante o cancelamento de uma reserva individual. O erro é logado mas
   * não interrompe o processamento das demais reservas.
   *
   * @param reservation Reserva que falhou ao ser cancelada
   * @param e Exceção que ocorreu
   */
  private void handleCancellationError(Reservation reservation, Exception e) {
    LOGGER.error(
        "Erro ao cancelar reserva {}. Continuando com as demais. Erro: {}",
        reservation.getId(),
        e.getMessage(),
        e);
  }
}
