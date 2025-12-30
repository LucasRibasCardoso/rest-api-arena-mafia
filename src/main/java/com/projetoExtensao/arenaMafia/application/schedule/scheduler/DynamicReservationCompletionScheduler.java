package com.projetoExtensao.arenaMafia.application.schedule.scheduler;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CompleteReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class DynamicReservationCompletionScheduler {

  private static final Logger logger =
      LoggerFactory.getLogger(DynamicReservationCompletionScheduler.class);
  private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

  private final TaskScheduler taskScheduler;
  private final CompleteReservationUseCase completeReservationUseCase;
  private final ReservationRepositoryPort reservationRepositoryPort;
  private final Map<UUID, ScheduledFuture<?>> scheduledTasks;

  public DynamicReservationCompletionScheduler(
      TaskScheduler taskScheduler,
      CompleteReservationUseCase completeReservationUseCase,
      ReservationRepositoryPort reservationRepositoryPort) {
    this.taskScheduler = taskScheduler;
    this.completeReservationUseCase = completeReservationUseCase;
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.scheduledTasks = new ConcurrentHashMap<>();
  }

  /**
   * Agenda a conclusão automática de uma reserva para o momento exato do seu término.
   *
   * @param reservationId ID da reserva
   * @param endDateTime data e hora de término da reserva
   */
  public void scheduleCompletion(UUID reservationId, LocalDateTime endDateTime) {
    Instant executionTime = endDateTime.atZone(ZONE_ID).toInstant();

    ScheduledFuture<?> scheduledTask =
        taskScheduler.schedule(() -> completeReservationIfEligible(reservationId), executionTime);

    scheduledTasks.put(reservationId, scheduledTask);
    logger.info("Agendada conclusão da reserva {} para {}", reservationId, endDateTime);
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
   * Reagenda todas as reservas confirmadas existentes quando a aplicação é iniciada. Garante que
   * reservas criadas antes da inicialização da aplicação também sejam completadas automaticamente.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void rescheduleExistingReservations() {
    logger.info("INICIANDO: Reagendamento de reservas confirmadas existentes");

    LocalDateTime now = LocalDateTime.now(ZONE_ID);
    List<Reservation> confirmedReservations =
        reservationRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(now);

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
          scheduleCompletion(reservation.getId(), endDateTime);
        });

    logger.info(
        "CONCLUÍDO: {} reservas confirmadas foram reagendadas", confirmedReservations.size());
  }
}
