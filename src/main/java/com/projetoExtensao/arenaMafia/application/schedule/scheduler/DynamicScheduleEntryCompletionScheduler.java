package com.projetoExtensao.arenaMafia.application.schedule.scheduler;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.DeleteBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CompleteReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class DynamicScheduleEntryCompletionScheduler {

  private static final Logger logger =
      LoggerFactory.getLogger(DynamicScheduleEntryCompletionScheduler.class);
  private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

  private final TaskScheduler taskScheduler;
  private final CompleteReservationUseCase completeReservationUseCase;
  private final DeleteBlockedTimeUseCase deleteBlockedTimeUseCase;
  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final Map<UUID, ScheduledFuture<?>> scheduledTasks;

  public DynamicScheduleEntryCompletionScheduler(
      TaskScheduler taskScheduler,
      CompleteReservationUseCase completeReservationUseCase,
      DeleteBlockedTimeUseCase deleteBlockedTimeUseCase,
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort) {
    this.taskScheduler = taskScheduler;
    this.completeReservationUseCase = completeReservationUseCase;
    this.deleteBlockedTimeUseCase = deleteBlockedTimeUseCase;
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.scheduledTasks = new ConcurrentHashMap<>();
  }

  /**
   * Agenda a conclusão automática de uma reserva para o momento exato do seu término.
   *
   * @param reservationId ID da reserva
   * @param endDateTime data e hora de término da reserva
   */
  public void scheduleReservationCompletion(UUID reservationId, LocalDateTime endDateTime) {
    Instant executionTime = endDateTime.atZone(ZONE_ID).toInstant();

    ScheduledFuture<?> scheduledTask =
        taskScheduler.schedule(() -> completeReservationIfEligible(reservationId), executionTime);

    scheduledTasks.put(reservationId, scheduledTask);
    logger.info("Agendada conclusão da reserva {} para {}", reservationId, endDateTime);
  }

  /**
   * Agenda a deleção automática de um BlockedTime para o momento exato do seu termino
   *
   * @param blockedTimeId ID do BlockedTime
   * @param endDateTime data e hora de término do blockedTime
   */
  public void scheduleBlockedTimeDeletion(UUID blockedTimeId, LocalDateTime endDateTime) {
    Instant executionTime = endDateTime.atZone(ZONE_ID).toInstant();

    ScheduledFuture<?> scheduledTask =
        taskScheduler.schedule(() -> deleteBlockedTimeIfEligible(blockedTimeId), executionTime);
    scheduledTasks.put(blockedTimeId, scheduledTask);
    logger.info("Agendada deleção do bloqueio {} para {}", blockedTimeId, endDateTime);
  }

  /**
   * Reagenda todas as reservas confirmadas existentes quando a aplicação é iniciada. Garante que
   * reservas criadas antes da inicialização da aplicação também sejam completadas automaticamente.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void rescheduleExistingReservations() {
    logger.info("INICIANDO: Reagendamento de reservas confirmadas existentes");

    LocalDateTime now = LocalDateTime.now(ZONE_ID);
    List<Reservation> expiredReservations =
        scheduleEntryRepositoryPort.findAllActiveSchedulesEndedBeforeOrEqual(now).stream()
            .filter(scheduleEntry -> scheduleEntry instanceof Reservation)
            .map(scheduleEntry -> (Reservation) scheduleEntry)
            .toList();

    for (Reservation reservation : expiredReservations) {
      try {
        completeReservationUseCase.execute(reservation.getId());
      } catch (Exception e) {
        logger.warn(
            "Falha ao completar reserva {} durante startup: {}",
            reservation.getId(),
            e.getMessage());
      }
    }

    List<Reservation> confirmedReservations =
        scheduleEntryRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(now).stream()
            .filter(scheduleEntry -> scheduleEntry instanceof Reservation)
            .map(scheduleEntry -> (Reservation) scheduleEntry)
            .toList();

    if (confirmedReservations.isEmpty()) {
      logger.info("Nenhuma reserva confirmada encontrada para reagendar");
      return;
    }

    confirmedReservations.forEach(
        reservation -> {
          LocalDateTime endDateTime =
              LocalDateTime.of(
                  reservation.getDateTimeSlot().date(),
                  reservation.getDateTimeSlot().timeInterval().endTime());
          scheduleReservationCompletion(reservation.getId(), endDateTime);
        });

    logger.info(
        "CONCLUÍDO: {} reservas confirmadas foram reagendadas", confirmedReservations.size());
  }


  /**
   * Reagenda todos os bloqueios confirmados existentes quando a aplicação é iniciada. Garante que blockedTimes
   * criados antes da inicialização da aplicação também sejam deletados automaticamente.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void rescheduleExistingBlockedTimes() {
    logger.info("INICIANDO: Reagendamento de bloqueios existentes");

    LocalDateTime now = LocalDateTime.now(ZONE_ID);
    List<BlockedTime> expiredBlockedTimes =
        scheduleEntryRepositoryPort.findAllActiveSchedulesEndedBeforeOrEqual(now).stream()
            .filter(scheduleEntry -> scheduleEntry instanceof BlockedTime)
            .map(scheduleEntry -> (BlockedTime) scheduleEntry)
            .toList();

    for (BlockedTime blockedTime : expiredBlockedTimes) {
      try {
        deleteBlockedTimeUseCase.execute(blockedTime.getId(), true);
      } catch (Exception e) {
        logger.warn(
            "Falha ao deletar bloqueio {} durante startup: {}",
            blockedTime.getId(),
            e.getMessage());
      }
    }

    List<BlockedTime> confirmedBlockedTimes =
        scheduleEntryRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(now).stream()
            .filter(scheduleEntry -> scheduleEntry instanceof BlockedTime)
            .map(scheduleEntry -> (BlockedTime) scheduleEntry)
            .toList();

    if (confirmedBlockedTimes.isEmpty()) {
      logger.info("Nenhum bloqueio confirmado encontrado para reagendar");
      return;
    }

    confirmedBlockedTimes.forEach(
        blockedTime -> {
          LocalDateTime endDateTime =
              LocalDateTime.of(
                  blockedTime.getDateTimeSlot().date(),
                  blockedTime.getDateTimeSlot().timeInterval().endTime());
          scheduleBlockedTimeDeletion(blockedTime.getId(), endDateTime);
        });

    logger.info(
        "CONCLUÍDO: {} bloqueios confirmados foram reagendados", confirmedBlockedTimes.size());
  }

  /**
   * Tenta completar uma reserva se ela estiver elegível (status CONFIRMED). Este metodo é chamado
   * pela tarefa agendada no momento do término da reserva.
   *
   * @param reservationId ID da reserva a ser completada
   */
  private void completeReservationIfEligible(UUID reservationId) {
    try {
      completeReservationUseCase.execute(reservationId);
      logger.info("Reserva {} concluída automaticamente", reservationId);
    } catch (Exception e) {
      logger.debug(
              "Reserva {} não pôde ser concluída automaticamente: {}", reservationId, e.getMessage());
    } finally {
      scheduledTasks.remove(reservationId);
    }
  }

  /**
   * Deletar um blockedTime ao termino
   *
   * @param blockedTimeId Identificador do blockedTime a ser deletado
   */
  private void deleteBlockedTimeIfEligible(UUID blockedTimeId) {
    try {
      deleteBlockedTimeUseCase.execute(blockedTimeId, true);
      logger.info("Bloqueio {} deletado automaticamente", blockedTimeId);
    } catch (Exception e) {
      logger.debug(
              "Bloqueio {} não pôde ser deletado automaticamente: {}", blockedTimeId, e.getMessage());
    } finally {
      scheduledTasks.remove(blockedTimeId);
    }
  }
}
