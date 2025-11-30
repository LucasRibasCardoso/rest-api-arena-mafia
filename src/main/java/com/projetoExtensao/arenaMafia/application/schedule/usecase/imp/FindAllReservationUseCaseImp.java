package com.projetoExtensao.arenaMafia.application.schedule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.FindAllReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FindAllReservationUseCaseImp implements FindAllReservationUseCase {

  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;

  public FindAllReservationUseCaseImp(ScheduleEntryRepositoryPort scheduleEntryRepositoryPort) {
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
  }

  @Override
  public Page<Reservation> execute(UUID userId, Pageable pageable) {
    return scheduleEntryRepositoryPort.findReservationsByUserId(userId, pageable);
  }
}
