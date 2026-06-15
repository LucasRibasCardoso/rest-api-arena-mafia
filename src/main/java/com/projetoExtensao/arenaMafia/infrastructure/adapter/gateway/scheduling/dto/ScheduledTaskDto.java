package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.scheduling.dto;

import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import java.util.UUID;

public record ScheduledTaskDto(UUID scheduleEntryId, ScheduleEntryType scheduleEntryType) {

  public boolean isReservation() {
    return scheduleEntryType == ScheduleEntryType.RESERVATION;
  }

  public boolean isBlockedTime() {
    return scheduleEntryType == ScheduleEntryType.BLOCKED_TIME;
  }
}
