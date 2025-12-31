package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.usecase.DisableCourtUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtCannotBeDisabledException;
import com.projetoExtensao.arenaMafia.domain.model.Court;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisableCourtUseCaseImp implements DisableCourtUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final ReservationRepositoryPort reservationRepositoryPort;

  public DisableCourtUseCaseImp(
      CourtRepositoryPort courtRepositoryPort,
      ReservationRepositoryPort reservationRepositoryPort) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.reservationRepositoryPort = reservationRepositoryPort;
  }

  @Override
  public void execute(UUID courtId) {
    boolean hasFutureReservations =
        reservationRepositoryPort.existsConfirmedReservationsAfter(courtId, LocalDate.now());

    if (hasFutureReservations) {
      throw new CourtCannotBeDisabledException();
    }

    Court court = courtRepositoryPort.findByIdOrElseThrow(courtId);
    court.disable();
    courtRepositoryPort.save(court);
  }
}
