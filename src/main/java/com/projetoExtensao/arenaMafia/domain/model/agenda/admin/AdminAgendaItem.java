package com.projetoExtensao.arenaMafia.domain.model.agenda.admin;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

public sealed interface AdminAgendaItem
    permits AdminAvailableSlotAgendaItem, AdminScheduleEntryAgendaItem {
  TimeInterval getTimeInterval();
}
