package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.scheduling.consumer;

import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.DeleteBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CompleteReservationUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.scheduling.dto.ScheduledTaskDto;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTaskConsumer {

  private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskConsumer.class);

  private final DeleteBlockedTimeUseCase deleteBlockedTimeUseCase;
  private final CompleteReservationUseCase completeReservationUseCase;

  public ScheduledTaskConsumer(
      CompleteReservationUseCase completeReservationUseCase,
      DeleteBlockedTimeUseCase deleteBlockedTimeUseCase) {
    this.deleteBlockedTimeUseCase = deleteBlockedTimeUseCase;
    this.completeReservationUseCase = completeReservationUseCase;
  }

  @SqsListener("${app.queue.schedule-task-queue}")
  public void consume(ScheduledTaskDto taskDto) {
    if (taskDto.isReservation()) {
      completeReservation(taskDto);
      logger.info("SUCESSO: Reserva {} marcada como COMPLETED.", taskDto.scheduleEntryId());
    } else if (taskDto.isBlockedTime()) {
      deleteBlockedTime(taskDto);
      logger.info("SUCESSO: BlockedTime {} deletado após conclusão.", taskDto.scheduleEntryId());
    } else {
      logger.warn(
          "Tipo de ScheduleEntry desconhecido recebido da fila: {}", taskDto.scheduleEntryType());
    }
  }

  private void completeReservation(ScheduledTaskDto taskDto) {
    try {
      completeReservationUseCase.execute(taskDto.scheduleEntryId());
    } catch (Exception e) {
      logger.error(
          "Erro ao completar reserva {}: {}. Redrive para DLQ.",
          taskDto.scheduleEntryId(),
          e.getMessage());
      throw e;
    }
  }

  private void deleteBlockedTime(ScheduledTaskDto taskDto) {
    try {
      deleteBlockedTimeUseCase.execute(taskDto.scheduleEntryId(), false);
    } catch (Exception e) {
      logger.error(
          "Erro ao deletar BlockedTime {}: {}. Redrive para DLQ.",
          taskDto.scheduleEntryId(),
          e.getMessage());
      throw e;
    }
  }
}
