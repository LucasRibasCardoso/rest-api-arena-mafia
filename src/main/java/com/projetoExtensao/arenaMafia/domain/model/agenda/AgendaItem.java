package com.projetoExtensao.arenaMafia.domain.model.agenda;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

public sealed interface AgendaItem permits ScheduleEntryAgendaItem, AvailableSlotAgendaItem, GroupedAvailableSlotAgendaItem {

  TimeInterval getTimeInterval();
}
