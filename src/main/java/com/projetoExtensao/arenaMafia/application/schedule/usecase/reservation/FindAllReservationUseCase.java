package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FindAllReservationUseCase {

  Page<Reservation> execute(UUID userId, Pageable pageable);
}
