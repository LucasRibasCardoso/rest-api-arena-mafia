package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.FindAllReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindAllReservationUseCaseImp implements FindAllReservationUseCase {

  private final ReservationRepositoryPort reservationRepositoryPort;

  public FindAllReservationUseCaseImp(ReservationRepositoryPort reservationRepositoryPort) {
    this.reservationRepositoryPort = reservationRepositoryPort;
  }

  @Override
  public Page<Reservation> execute(UUID userId, Pageable pageable) {
    return reservationRepositoryPort.findReservationsByUserId(userId, pageable);
  }
}
