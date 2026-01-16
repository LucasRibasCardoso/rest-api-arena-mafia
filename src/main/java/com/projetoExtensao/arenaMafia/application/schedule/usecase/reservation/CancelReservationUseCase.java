package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation;

import java.util.UUID;

public interface CancelReservationUseCase {
  void execute(UUID authenticatedUserId, UUID reservationId);
}
