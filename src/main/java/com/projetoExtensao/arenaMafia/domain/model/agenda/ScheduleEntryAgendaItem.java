package com.projetoExtensao.arenaMafia.domain.model.agenda;

import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

public record ScheduleEntryAgendaItem(ScheduleEntry scheduleEntry) implements AgendaItem {

  @Override
  public TimeInterval getTimeInterval() {
    return scheduleEntry.getDateTimeSlot().timeInterval();
  }
}

