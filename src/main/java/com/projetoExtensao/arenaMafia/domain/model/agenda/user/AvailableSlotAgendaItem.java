package com.projetoExtensao.arenaMafia.domain.model.agenda.user;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record AvailableSlotAgendaItem(
    TimeInterval timeInterval, Set<UUID> modalityIds, BigDecimal price) implements AgendaItem {

  @Override
  public TimeInterval getTimeInterval() {
    return this.timeInterval;
  }
}
