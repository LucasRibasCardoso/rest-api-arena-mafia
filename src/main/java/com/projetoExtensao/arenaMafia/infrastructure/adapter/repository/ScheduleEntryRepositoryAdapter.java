package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ScheduleEntryMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ScheduleEntryJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ScheduleEntryRepositoryAdapter implements ScheduleEntryRepositoryPort {

  private final ScheduleEntryMapper scheduleEntryMapper;
  private final ScheduleEntryJpaRepository scheduleEntryJpaRepository;

  public ScheduleEntryRepositoryAdapter(
      ScheduleEntryMapper scheduleEntryMapper,
      ScheduleEntryJpaRepository scheduleEntryJpaRepository) {
    this.scheduleEntryMapper = scheduleEntryMapper;
    this.scheduleEntryJpaRepository = scheduleEntryJpaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ScheduleEntry> findById(UUID id) {
    return scheduleEntryJpaRepository.findById(id).map(scheduleEntryMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ScheduleEntry> findConfirmedSchedulesByCourtAndDate(UUID courtId, LocalDate date) {
    return scheduleEntryJpaRepository
        .findConfirmedReservationsByCourtAndDate(courtId, date)
        .stream()
        .map(scheduleEntryMapper::toDomain)
        .toList();
  }
}
