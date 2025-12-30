package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CompleteReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CompleteReservationUseCaseImp implements CompleteReservationUseCase {

  private final ReservationRepositoryPort reservationRepositoryPort;

  public CompleteReservationUseCaseImp(ReservationRepositoryPort reservationRepositoryPort) {
    this.reservationRepositoryPort = reservationRepositoryPort;
  }

  @Override
  public void execute(UUID reservationId) {
    ScheduleEntry scheduleEntry = reservationRepositoryPort.findByIdOrElseThrow(reservationId);

    if (!(scheduleEntry instanceof Reservation reservation)) {
      return;
    }

    if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
      reservation.complete();
      reservationRepositoryPort.save(reservation);
    }
  }
}
