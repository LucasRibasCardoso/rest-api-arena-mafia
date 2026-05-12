package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request.AdminReservationSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminListReservationsUseCase {

  Page<ReservationDetail> execute(AdminReservationSearchRequestDto requestDto, Pageable pageable);
}
