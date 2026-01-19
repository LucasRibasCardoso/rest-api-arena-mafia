package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.UUID;

public interface FindByIdReservationUseCase {
  ReservationDetail execute(UUID userId, UUID reservationId);
}
