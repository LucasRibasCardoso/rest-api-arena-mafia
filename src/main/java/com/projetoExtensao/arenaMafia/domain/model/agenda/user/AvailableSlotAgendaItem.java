package com.projetoExtensao.arenaMafia.domain.model.agenda.user;

import com.projetoExtensao.arenaMafia.domain.valueobjects.AvailableSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

public record AvailableSlotAgendaItem(AvailableSlot availableSlot) implements AgendaItem {

  @Override
  public TimeInterval getTimeInterval() {
    return availableSlot.timeInterval();
  }
}
