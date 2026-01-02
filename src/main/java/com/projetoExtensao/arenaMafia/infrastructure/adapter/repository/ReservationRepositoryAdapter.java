package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ScheduleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
  public List<Reservation> findAllByIds(List<UUID> ids) {
    return scheduleEntryJpaRepository.findAllById(ids).stream()
        .map(entity -> (Reservation) scheduleEntryMapper.toDomain(entity))
        .toList();
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

  @Override
  @Transactional(readOnly = true)
  public boolean existsConfirmedReservationsAfter(UUID courtId, LocalDate date) {
    return scheduleEntryJpaRepository.existsConfirmedReservationsAfter(courtId, date);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsConfirmedReservationsForDaysAndTime(
      Set<DayOfWeek> daysOfWeek, LocalTime startTime, LocalTime endTime, LocalDate afterDate) {

    if (daysOfWeek == null || daysOfWeek.isEmpty()) return false;

    Set<Integer> currentSqlDays =
        daysOfWeek.stream().map(DayOfWeek::getSqlDayOfWeekValue).collect(Collectors.toSet());

    boolean crossesMidnight = endTime.isBefore(startTime);

    if (!crossesMidnight) {
      // CENÁRIO SIMPLES (Ex: 08:00 as 22:00)
      return scheduleEntryJpaRepository.existsConflictInDays(
          afterDate, currentSqlDays, startTime, endTime);
    } else {
      // CENÁRIO COMPLEXO (Ex: 22:00 as 02:00)
      // Parte A: Verificar o final da noite nos dias originais (22:00 -> 23:59:59.999)
      boolean conflictPart1 =
          scheduleEntryJpaRepository.existsConflictInDays(
              afterDate, currentSqlDays, startTime, LocalTime.MAX);

      if (conflictPart1) return true;

      Set<Integer> nextDaySqlDays =
          daysOfWeek.stream()
              .map(DayOfWeek::next)
              .map(DayOfWeek::getSqlDayOfWeekValue)
              .collect(Collectors.toSet());

      return scheduleEntryJpaRepository.existsConflictInDays(
          afterDate, nextDaySqlDays, LocalTime.MIN, endTime);
    }
  }

  @Override
  public List<Reservation> findAllConfirmedReservationsWithEndTimeBeforeOrEqual(
      LocalDateTime dateTime) {
    return scheduleEntryJpaRepository
        .findConfirmedReservationsEndedBeforeOrEqual(dateTime.toLocalDate(), dateTime.toLocalTime())
        .stream()
        .map(entity -> (Reservation) scheduleEntryMapper.toDomain(entity))
        .toList();
  }
}
