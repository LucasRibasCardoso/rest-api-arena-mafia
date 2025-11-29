package com.projetoExtensao.arenaMafia.application.schedule.usecase;

import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;

import java.util.UUID;

public interface FindByIdReservationUseCase {
  ScheduleEntry execute(UUID userId, UUID reservationId);
}
