package com.projetoExtensao.arenaMafia.application.schedule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.CompleteReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CompleteReservationUseCaseImp implements CompleteReservationUseCase {

  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;

  public CompleteReservationUseCaseImp(ScheduleEntryRepositoryPort scheduleEntryRepositoryPort) {
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
  }

  @Override
  public void execute(UUID reservationId) {
    ScheduleEntry scheduleEntry = scheduleEntryRepositoryPort.findByIdOrElseThrow(reservationId);

    if (!(scheduleEntry instanceof Reservation reservation)) {
      return;
    }

    if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
      reservation.complete();
      scheduleEntryRepositoryPort.save(reservation);
    }
  }
}
