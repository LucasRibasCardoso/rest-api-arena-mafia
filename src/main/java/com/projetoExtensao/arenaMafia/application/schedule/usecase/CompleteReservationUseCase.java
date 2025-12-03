package com.projetoExtensao.arenaMafia.application.schedule.usecase;

import java.util.UUID;

public interface CompleteReservationUseCase {
  void execute(UUID reservationId);
}
