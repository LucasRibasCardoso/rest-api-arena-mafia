package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.UUID;

public interface FindByIdReservationUseCase {
  Reservation execute(UUID userId, UUID reservationId);
}
