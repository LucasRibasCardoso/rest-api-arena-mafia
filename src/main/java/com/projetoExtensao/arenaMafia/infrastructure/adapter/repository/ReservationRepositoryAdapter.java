package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ScheduleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ScheduleEntryMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ScheduleEntryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReservationRepositoryAdapter implements ReservationRepositoryPort {

  private final ScheduleEntryJpaRepository scheduleEntryJpaRepository;
  private final ScheduleEntryMapper scheduleEntryMapper;

  public ReservationRepositoryAdapter(
      ScheduleEntryJpaRepository scheduleEntryJpaRepository,
      ScheduleEntryMapper scheduleEntryMapper) {
    this.scheduleEntryJpaRepository = scheduleEntryJpaRepository;
    this.scheduleEntryMapper = scheduleEntryMapper;
  }

  @Override
  @Transactional
  public Reservation save(Reservation reservation) {
    var entity = scheduleEntryMapper.toEntity(reservation);
    var savedEntity = scheduleEntryJpaRepository.save(entity);
    return (Reservation) scheduleEntryMapper.toDomain(savedEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Reservation> findById(UUID id) {
    return scheduleEntryJpaRepository
        .findById(id)
        .map(scheduleEntryMapper::toDomain)
        .filter(scheduleEntry -> scheduleEntry instanceof Reservation)
        .map(scheduleEntry -> (Reservation) scheduleEntry);
  }

  @Override
  @Transactional(readOnly = true)
  public Reservation findByIdOrElseThrow(UUID id) {
    return findById(id)
        .orElseThrow(() -> new ScheduleNotFoundException(ErrorCode.SCHEDULE_ENTRY_NOT_FOUND));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Reservation> findReservationsByUserId(UUID userId, Pageable pageable) {
    return scheduleEntryJpaRepository
        .findReservationsByUserId(userId, pageable)
        .map(entity -> (Reservation) scheduleEntryMapper.toDomain(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Reservation findReservationByIdAndUserIdOrElseThrow(UUID reservationId, UUID userId) {
    return scheduleEntryJpaRepository
        .findReservationByIdAndUser(reservationId, userId)
        .map(entity -> (Reservation) scheduleEntryMapper.toDomain(entity))
        .orElseThrow(() -> new ScheduleNotFoundException(ErrorCode.SCHEDULE_ENTRY_NOT_FOUND));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Reservation> findAllConfirmedReservationsWithEndTimeAfter(LocalDateTime dateTime) {
    LocalDate date = dateTime.toLocalDate();
    LocalTime time = dateTime.toLocalTime();

    return scheduleEntryJpaRepository.findConfirmedReservationsEndedAfter(date, time).stream()
        .map(entity -> (Reservation) scheduleEntryMapper.toDomain(entity))
        .toList();
  }
}
