package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ScheduleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ScheduleEntryEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ScheduleEntryMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ScheduleEntryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        .findConfirmedReservationsByCourtAndDate(courtId, date)
        .stream()
        .map(scheduleEntryMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Reservation> findReservationsByUserId(UUID userId, Pageable pageable) {
    return scheduleEntryJpaRepository
        .findReservationsByUserId(userId, pageable)
        .map(entity -> (Reservation) scheduleEntryMapper.toDomain(entity));
  }
}
