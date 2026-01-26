package com.projetoExtensao.arenaMafia.domain.model.agenda.user;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidAgendaItemException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.util.Set;
import java.util.UUID;

public record GroupedAvailableSlotAgendaItem(TimeInterval timeInterval, Set<UUID> availableModalityIds) implements AgendaItem {

  public GroupedAvailableSlotAgendaItem {
    if (timeInterval == null) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_REQUIRED);
    }
    if (availableModalityIds == null || availableModalityIds.isEmpty()) {
      throw new InvalidAgendaItemException(ErrorCode.AVAILABLE_MODALITY_IDS_REQUIRED);
    }
    availableModalityIds = Set.copyOf(availableModalityIds);
  }

  @Override
  public TimeInterval getTimeInterval() {
    return timeInterval;
  }
}
