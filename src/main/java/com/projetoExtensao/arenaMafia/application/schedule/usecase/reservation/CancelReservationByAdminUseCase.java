package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation;

import java.util.UUID;

public interface CancelReservationByAdminUseCase {

  void execute(UUID adminId, UUID reservationId, boolean cancelAllRecurring);
}
