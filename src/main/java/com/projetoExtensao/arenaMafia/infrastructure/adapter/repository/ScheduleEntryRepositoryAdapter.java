package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ScheduleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ScheduleEntryEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ScheduleEntryMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ScheduleEntryJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
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
  @Transactional
  public ScheduleEntry save(ScheduleEntry scheduleEntry) {
    ScheduleEntryEntity entity = scheduleEntryMapper.toEntity(scheduleEntry);
    ScheduleEntryEntity savedEntity = scheduleEntryJpaRepository.save(entity);
    return scheduleEntryMapper.toDomain(savedEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public ScheduleEntry findByIdOrElseThrow(UUID id) {
    return scheduleEntryJpaRepository
        .findById(id)
        .map(scheduleEntryMapper::toDomain)
        .orElseThrow(() -> new ScheduleNotFoundException(ErrorCode.SCHEDULE_ENTRY_NOT_FOUND));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ScheduleEntry> findConfirmedSchedulesByCourtAndDate(UUID courtId, LocalDate date) {
    return scheduleEntryJpaRepository
        .findSchedulesByCourtAndDate(courtId, date)
        .stream()
        .map(scheduleEntryMapper::toDomain)
        .filter(ScheduleEntry::isActive)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ScheduleEntry> findAllSchedulesByDate(LocalDate date) {
    return scheduleEntryJpaRepository
        .findAllSchedulesByDate(date)
        .stream()
        .map(scheduleEntryMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ScheduleEntry> findConflicts(
      List<UUID> courtIds,
      LocalDate startDate,
      LocalDate endDate,
      TimeInterval timeInterval) {
    var entities = scheduleEntryJpaRepository.findActiveSchedulesByCourtAndDateRange(courtIds, startDate, endDate);

    return entities.stream()
        .map(scheduleEntryMapper::toDomain)
        .filter(scheduleEntry -> scheduleEntry.getDateTimeSlot().timeInterval().overlaps(timeInterval))
        .toList();
  }
}

