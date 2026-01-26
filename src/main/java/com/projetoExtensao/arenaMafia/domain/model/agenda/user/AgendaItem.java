package com.projetoExtensao.arenaMafia.domain.model.agenda.user;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

public sealed interface AgendaItem
    permits ScheduleEntryAgendaItem,
        AvailableSlotAgendaItem,
        GroupedAvailableSlotAgendaItem,
        GroupedBlockedTimeAgendaItem {

  TimeInterval getTimeInterval();
}
