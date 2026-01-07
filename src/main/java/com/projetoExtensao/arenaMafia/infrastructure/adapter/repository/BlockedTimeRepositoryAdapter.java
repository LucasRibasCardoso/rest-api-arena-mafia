package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.BlockedTimeNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.BlockedTimeEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ScheduleEntryMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ScheduleEntryJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BlockedTimeRepositoryAdapter implements BlockedTimeRepositoryPort {

  private final ScheduleEntryJpaRepository scheduleEntryJpaRepository;
  private final ScheduleEntryMapper scheduleEntryMapper;

  public BlockedTimeRepositoryAdapter(
      ScheduleEntryJpaRepository scheduleEntryJpaRepository,
      ScheduleEntryMapper scheduleEntryMapper) {
    this.scheduleEntryJpaRepository = scheduleEntryJpaRepository;
    this.scheduleEntryMapper = scheduleEntryMapper;
  }

  @Override
  public BlockedTime save(BlockedTime blockedTime) {
    BlockedTimeEntity entity = (BlockedTimeEntity) scheduleEntryMapper.toEntity(blockedTime);
    BlockedTimeEntity savedEntity = scheduleEntryJpaRepository.save(entity);
    return (BlockedTime) scheduleEntryMapper.toDomain(savedEntity);
  }

  @Override
  public List<BlockedTime> saveAll(List<BlockedTime> blockedTimes) {
    List<BlockedTimeEntity> entities =
        blockedTimes.stream()
            .map(blockedTime -> (BlockedTimeEntity) scheduleEntryMapper.toEntity(blockedTime))
            .toList();

    List<BlockedTimeEntity> savedEntities = scheduleEntryJpaRepository.saveAll(entities);

    return savedEntities.stream()
        .map(entity -> (BlockedTime) scheduleEntryMapper.toDomain(entity))
        .toList();
  }

  @Override
  public Optional<BlockedTime> findById(UUID id) {
    return scheduleEntryJpaRepository
        .findById(id)
        .filter(entity -> entity instanceof BlockedTimeEntity)
        .map(entity -> (BlockedTime) scheduleEntryMapper.toDomain(entity));
  }

  @Override
  public BlockedTime findByIdOrElseThrow(UUID id) {
    return findById(id).orElseThrow(BlockedTimeNotFoundException::new);
  }

  @Override
  public List<BlockedTime> findAllByRecurringBlockedTimeId(UUID recurringBlockedTimeId) {
    return scheduleEntryJpaRepository
        .findAllByRecurringBlockedTimeId(recurringBlockedTimeId)
        .stream()
        .map(entity -> (BlockedTime) scheduleEntryMapper.toDomain(entity))
        .toList();
  }

  @Override
  public List<BlockedTime> findByCourtIdAndDateRange(
      UUID courtId, LocalDate startDate, LocalDate endDate) {
    return scheduleEntryJpaRepository
        .findByCourtIdAndDateRange(courtId, startDate, endDate)
        .stream()
        .map(entity -> (BlockedTime) scheduleEntryMapper.toDomain(entity))
        .toList();
  }

  @Override
  public List<BlockedTime> findByCourtIdsAndDateRange(
      List<UUID> courtIds, LocalDate startDate, LocalDate endDate) {
    return scheduleEntryJpaRepository
        .findByCourtIdsAndDateRange(courtIds, startDate, endDate)
        .stream()
        .map(entity -> (BlockedTime) scheduleEntryMapper.toDomain(entity))
        .toList();
  }

  @Override
  public void deleteAllByIds(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    scheduleEntryJpaRepository.deleteAllById(ids);
  }

  @Override
  public void deleteByRecurringBlockedTimeId(UUID recurringBlockedTimeId) {
    scheduleEntryJpaRepository.deleteByRecurringBlockedTimeId(recurringBlockedTimeId);
  }

  @Override
  public boolean existsActiveBlockedTimeByCourtIdAndDate(UUID courtId, LocalDate date) {
    return scheduleEntryJpaRepository.existsByCourtIdAndDate(courtId, date);
  }
}
