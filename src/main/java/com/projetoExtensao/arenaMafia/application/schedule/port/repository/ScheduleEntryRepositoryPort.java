package com.projetoExtensao.arenaMafia.application.schedule.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ScheduleEntryRepositoryPort {

  ScheduleEntry save(ScheduleEntry scheduleEntry);

  ScheduleEntry findByIdOrElseThrow(UUID id);

  List<ScheduleEntry> findAllActiveSchedulesByCourtAndDate(UUID courtId, LocalDate date);

  List<ScheduleEntry> findAllActiveSchedulesByDate(LocalDate date);

  List<ScheduleEntry> findAllActiveSchedulesByCourtIdAfterDate(UUID courtId, LocalDate date);

  List<ScheduleEntry> findAllActiveSchedulesConflicts(
      List<UUID> courtIds,
      LocalDate startDate,
      LocalDate endDate,
      TimeInterval timeInterval,
      Set<DayOfWeek> selectedDaysOfWeek);
}
