package com.projetoExtensao.arenaMafia.application.schedule.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleEntryRepositoryPort {

  Optional<ScheduleEntry> findById(UUID id);

  List<ScheduleEntry> findConfirmedSchedulesByCourtAndDate(UUID courtId, LocalDate date);
}
