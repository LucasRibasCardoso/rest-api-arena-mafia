package com.projetoExtensao.arenaMafia.domain.model.agenda.admin;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ScheduleEntryDetail;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

public record AdminScheduleEntryAgendaItem(ScheduleEntryDetail detail) implements AdminAgendaItem {
  @Override
  public TimeInterval getTimeInterval() {
    return detail.timeInterval();
  }
}
