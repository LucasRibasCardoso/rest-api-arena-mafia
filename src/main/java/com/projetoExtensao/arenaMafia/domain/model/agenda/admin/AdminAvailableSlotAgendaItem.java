package com.projetoExtensao.arenaMafia.domain.model.agenda.admin;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AdminAvailableSlotAgendaItem(
    UUID courtId,
    String courtName,
    TimeInterval timeInterval,
    Set<UUID> availableModalityIds,
    BigDecimal price)
    implements AdminAgendaItem {

  @Override
  public TimeInterval getTimeInterval() {
    return timeInterval;
  }
}
