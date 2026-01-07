package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request.CreateReservationRequestDto;

import java.util.UUID;

public interface CreateReservationUseCase {

  Reservation execute(UUID userId, CreateReservationRequestDto request);
}
