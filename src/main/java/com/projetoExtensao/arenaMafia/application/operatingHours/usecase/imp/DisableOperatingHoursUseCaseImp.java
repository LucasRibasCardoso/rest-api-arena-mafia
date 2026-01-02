package com.projetoExtensao.arenaMafia.application.operatingHours.usecase.imp;

import com.projetoExtensao.arenaMafia.application.operatingHours.port.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.DisableOperatingHoursUseCase;

import java.time.LocalDate;
import java.util.UUID;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.OperatingHoursCannotBeDisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisableOperatingHoursUseCaseImp implements DisableOperatingHoursUseCase {

  private final OperatingHoursRepositoryPort operatingHoursRepository;
  private final ReservationRepositoryPort reservationRepository;

  public DisableOperatingHoursUseCaseImp(
      OperatingHoursRepositoryPort operatingHoursRepository,
      ReservationRepositoryPort reservationRepository) {
    this.operatingHoursRepository = operatingHoursRepository;
    this.reservationRepository = reservationRepository;
  }

  @Override
  public void execute(UUID hourId) {
    var operatingHours = operatingHoursRepository.findByIdOrElseThrow(hourId);

    boolean hasFutureReservations = reservationRepository.existsConfirmedReservationsForDaysAndTime(
            operatingHours.getDaysOfWeek(),
            operatingHours.getTimeInterval().startTime(),
            operatingHours.getTimeInterval().endTime(),
            LocalDate.now()
    );

    if (hasFutureReservations) {
      throw new OperatingHoursCannotBeDisabledException();
    }

    operatingHours.disable();
    operatingHoursRepository.save(operatingHours);
  }
}
