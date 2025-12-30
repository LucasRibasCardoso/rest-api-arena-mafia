package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation;

import java.util.UUID;

public interface CompleteReservationUseCase {
  void execute(UUID reservationId);
}
