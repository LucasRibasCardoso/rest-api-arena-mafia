package com.projetoExtensao.arenaMafia.application.scheduleTask.gateway;

import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ScheduledTaskPort {

  void cancelTask(UUID scheduleEntryId, ScheduleEntryType scheduleEntryType);

  void scheduleTask(UUID scheduleEntryId, ScheduleEntryType scheduleEntryType, LocalDateTime executionTime);

  void scheduleReservationReminderTask(UUID reservationId, LocalDateTime executionTime);

  void cancelReservationReminderTask(UUID reservationId);
}
