package com.projetoExtensao.arenaMafia.domain.model.agenda;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

import java.util.Set;
import java.util.UUID;

public record GroupedBlockedTimeAgendaItem(
    TimeInterval timeInterval, Set<UUID> blockedCourtIds, String description)
    implements AgendaItem {

  @Override
  public TimeInterval getTimeInterval() {
    return timeInterval;
  }
}
