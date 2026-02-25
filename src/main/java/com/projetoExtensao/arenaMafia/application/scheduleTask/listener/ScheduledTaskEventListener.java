package com.projetoExtensao.arenaMafia.application.scheduleTask.listener;

import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnBlockedTimeCreatedScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnBlockedTimeDeletedScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnReservationCancelledScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnReservationCreatedScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.application.scheduleTask.gateway.ScheduledTaskPort;
import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
public class ScheduledTaskEventListener {

  // Define quantas horas antes da reserva o lembrete deve ser enviado
  private static final int REMINDER_HOURS_BEFORE_RESERVATION = 2;

  private final ScheduledTaskPort scheduledTaskPort;

  public ScheduledTaskEventListener(ScheduledTaskPort scheduledTaskPort) {
    this.scheduledTaskPort = scheduledTaskPort;
  }


  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReservationCreated(OnReservationCreatedScheduleTaskEvent event) {
    Reservation res = event.reservation();
    DateTimeSlot dateTimeSlot = res.getDateTimeSlot();

    LocalDateTime completeExecutionTime = dateTimeSlot.getEndDateTime();
    LocalDateTime reminderExecutionTime = dateTimeSlot.getStartDateTime().minusHours(REMINDER_HOURS_BEFORE_RESERVATION);

    scheduledTaskPort.scheduleTask(res.getId(), ScheduleEntryType.RESERVATION, completeExecutionTime);
    scheduledTaskPort.scheduleReservationReminderTask(res.getId(), reminderExecutionTime);
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReservationCancelled(OnReservationCancelledScheduleTaskEvent event) {
    scheduledTaskPort.cancelTask(event.reservationId(), ScheduleEntryType.RESERVATION);
    scheduledTaskPort.cancelReservationReminderTask(event.reservationId());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onBlockedTimeCreated(OnBlockedTimeCreatedScheduleTaskEvent event) {
    BlockedTime blockedTime = event.blockedTime();
    LocalDateTime deleteExecutionTime = blockedTime.getDateTimeSlot().getEndDateTime();
    scheduledTaskPort.scheduleTask(blockedTime.getId(), ScheduleEntryType.BLOCKED_TIME, deleteExecutionTime);
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onBlockedTimeDeleted(OnBlockedTimeDeletedScheduleTaskEvent event) {
    scheduledTaskPort.cancelTask(event.blockedTimeId(), ScheduleEntryType.BLOCKED_TIME);
  }
}

