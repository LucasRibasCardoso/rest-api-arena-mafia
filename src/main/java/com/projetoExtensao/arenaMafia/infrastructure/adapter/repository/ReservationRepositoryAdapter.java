package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ScheduleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ReservationMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ReservationJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class ReservationRepositoryAdapter implements ReservationRepositoryPort {

  private final ReservationJpaRepository reservationJpaRepository;
  private final ReservationMapper reservationMapper;

  public ReservationRepositoryAdapter(
      ReservationJpaRepository reservationJpaRepository, ReservationMapper reservationMapper) {
    this.reservationJpaRepository = reservationJpaRepository;
    this.reservationMapper = reservationMapper;
  }

  @Override
  public Reservation save(Reservation reservation) {
    ReservationEntity entity = reservationMapper.toEntity(reservation);
    ReservationEntity savedEntity = reservationJpaRepository.save(entity);
    return reservationMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<Reservation> findById(UUID id) {
    return reservationJpaRepository.findById(id).map(reservationMapper::toDomain);
  }

  @Override
  public List<Reservation> findAllByIds(List<UUID> ids) {
    return reservationJpaRepository.findAllById(ids).stream()
        .map(reservationMapper::toDomain)
        .toList();
  }

  @Override
  public Reservation findByIdOrElseThrow(UUID id) {
    return findById(id)
        .orElseThrow(() -> new ScheduleNotFoundException(ErrorCode.SCHEDULE_ENTRY_NOT_FOUND));
  }

  @Override
  public Page<Reservation> findReservationsByUserId(UUID userId, Pageable pageable) {
    return reservationJpaRepository
        .findReservationsByUserId(userId, pageable)
        .map(reservationMapper::toDomain);
  }

  @Override
  public Reservation findReservationByIdAndUserIdOrElseThrow(UUID reservationId, UUID userId) {
    return reservationJpaRepository
        .findReservationByIdAndUser(reservationId, userId)
        .map(reservationMapper::toDomain)
        .orElseThrow(() -> new ScheduleNotFoundException(ErrorCode.SCHEDULE_ENTRY_NOT_FOUND));
  }
}
