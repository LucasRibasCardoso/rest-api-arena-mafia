package com.projetoExtensao.arenaMafia.application.schedule.usecase;

import java.util.UUID;

public interface CancelReservationUseCase {
  void execute(UUID authenticatedUserId, UUID reservationId);
}
