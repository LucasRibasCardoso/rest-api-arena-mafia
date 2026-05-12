package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request.AdminReservationCreateRequestDto;
import java.util.List;
import java.util.UUID;

public interface CreateReservationByAdminUseCase {

  List<ReservationDetail> execute(UUID adminId, AdminReservationCreateRequestDto requestDto);
}
