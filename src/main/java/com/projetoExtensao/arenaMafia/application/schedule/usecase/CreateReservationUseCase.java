package com.projetoExtensao.arenaMafia.application.schedule.usecase;

import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request.CreateReservationRequestDto;

import java.util.UUID;

public interface CreateReservationUseCase {

  ScheduleEntry execute(UUID userId, CreateReservationRequestDto request);
}
