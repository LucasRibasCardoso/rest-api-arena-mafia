package com.projetoExtensao.arenaMafia.application.schedule.service;

import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ScheduleDetail;
import com.projetoExtensao.arenaMafia.application.schedule.result.ScheduleEntriesEnrichedResult;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ScheduleEntryEnrichmentService {

  private final UserRepositoryPort userRepository;
  private final CourtRepositoryPort courtRepository;
  private final ModalityRepositoryPort modalityRepository;

  public ScheduleEntryEnrichmentService(
      UserRepositoryPort userRepository,
      CourtRepositoryPort courtRepository,
      ModalityRepositoryPort modalityRepository) {

    this.userRepository = userRepository;
    this.courtRepository = courtRepository;
    this.modalityRepository = modalityRepository;
  }

  /**
   * Enriquece uma lista de entradas de agendamento com detalhes adicionais. Usa polimorfismo para
   * tratar Reservations e BlockedTimes de forma unificada.
   *
   * @param scheduleEntries lista de entradas de agendamento a serem enriquecidas
   * @return listas de entradas enriquecidas
   */
  public ScheduleEntriesEnrichedResult enrichScheduleEntries(List<? extends ScheduleEntry> scheduleEntries) {
    if (scheduleEntries == null || scheduleEntries.isEmpty()) {
      return new ScheduleEntriesEnrichedResult(List.of(), List.of(), List.of());
    }

    // Separar reservas para buscar dados específicos (usuários e modalidades)
    List<Reservation> reservations =
        scheduleEntries.stream()
            .filter(entry -> entry instanceof Reservation)
            .map(entry -> (Reservation) entry)
            .toList();

    // Buscar dados necessários em batch
    Map<UUID, User> userMap = loadUsers(reservations);
    Map<UUID, Court> courtMap = loadCourts(scheduleEntries);
    Map<UUID, Modality> modalityMap = loadModalities(reservations);

    // Enriquecer cada entrada usando polimorfismo
    List<ScheduleDetail> scheduleDetails = scheduleEntries.stream()
        .map(entry -> enrichSingleEntry(entry, userMap, courtMap, modalityMap))
        .toList();

    // Separar os detalhes enriquecidos em listas específicas
    List<ReservationDetail> reservationDetails = scheduleDetails.stream()
        .filter(detail -> detail instanceof ReservationDetail)
        .map(detail -> (ReservationDetail) detail)
        .toList();

    List<BlockedTimeDetail> blockedTimeDetails = scheduleDetails.stream()
        .filter(detail -> detail instanceof BlockedTimeDetail)
        .map(detail -> (BlockedTimeDetail) detail)
        .toList();

    return new ScheduleEntriesEnrichedResult(scheduleDetails, reservationDetails, blockedTimeDetails);
  }

  /**
   * Enriquece uma única entrada de agendamento usando polimorfismo.
   *
   * @param entry entrada a ser enriquecida
   * @param userMap mapa de usuários
   * @param courtMap mapa de quadras
   * @param modalityMap mapa de modalidades
   * @return detalhes da entrada enriquecida
   */
  private ScheduleDetail enrichSingleEntry(
      ScheduleEntry entry,
      Map<UUID, User> userMap,
      Map<UUID, Court> courtMap,
      Map<UUID, Modality> modalityMap) {

    return switch (entry) {
      case Reservation reservation -> enrichReservation(reservation, userMap, courtMap, modalityMap);
      case BlockedTime blockedTime -> enrichBlockedTime(blockedTime, courtMap);
      default ->
          throw new IllegalStateException(
              "Tipo de ScheduleEntry não suportado: " + entry.getClass());
    };
  }

  private ReservationDetail enrichReservation(
      Reservation reservation,
      Map<UUID, User> userMap,
      Map<UUID, Court> courtMap,
      Map<UUID, Modality> modalityMap) {

    User user = userMap.get(reservation.getUserId());
    Court court = courtMap.get(reservation.getCourtId());
    Modality modality = modalityMap.get(reservation.getModalityId());

    return new ReservationDetail(
        reservation.getId(),
        reservation.getUserId(),
        reservation.getCourtId(),
        user.getFullName() != null ? user.getFullName() : "Usuário Desconhecido",
        user.getPhone() != null ? user.getPhone() : "Telefone Não Informado",
        court.getName() != null ? court.getName() : "Quadra Desconhecida",
        reservation.getDateTimeSlot().date(),
        reservation.getDateTimeSlot().timeInterval(),
        modality.getName() != null ? modality.getName() : "Modalidade Desconhecida",
        reservation.getPrice(),
        reservation.getStatus(),
        reservation.getRecurringReservationId());
  }

  private BlockedTimeDetail enrichBlockedTime(
          BlockedTime blockedTime,
          Map<UUID, Court> courtMap
  ) {

    Court court = courtMap.get(blockedTime.getCourtId());

    return new BlockedTimeDetail(
        blockedTime.getId(),
        blockedTime.getCourtId(),
        court.getName() != null ? court.getName() : "Quadra Desconhecida",
        blockedTime.getDateTimeSlot().date(),
        blockedTime.getDateTimeSlot().timeInterval(),
        blockedTime.getDescription(),
        blockedTime.isFullDay(),
        blockedTime.getRecurringBlockedTimeId());
  }

  private Map<UUID, User> loadUsers(List<Reservation> reservations) {
    if (reservations.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<UUID> userIds =
            reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());

    return userRepository.findAllByIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
  }

  private Map<UUID, Court> loadCourts(List<? extends ScheduleEntry> entries) {
    if (entries.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<UUID> courtIds =
            entries.stream().map(ScheduleEntry::getCourtId).collect(Collectors.toSet());

    return courtRepository.findAllActiveByIds(courtIds).stream()
            .collect(Collectors.toMap(Court::getId, Function.identity()));
  }

  private Map<UUID, Modality> loadModalities(List<Reservation> reservations) {
    if (reservations.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<UUID> modalityIds =
            reservations.stream().map(Reservation::getModalityId).collect(Collectors.toSet());

    return modalityRepository.findAllByIds(modalityIds).stream()
            .collect(Collectors.toMap(Modality::getId, Function.identity()));
  }
}
