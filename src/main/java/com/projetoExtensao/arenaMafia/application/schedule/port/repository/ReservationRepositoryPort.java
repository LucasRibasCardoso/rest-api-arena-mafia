package com.projetoExtensao.arenaMafia.application.schedule.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ReservationRepositoryPort {

  Reservation save(Reservation reservation);

  void saveAll(List<Reservation> reservations);

  Page<Reservation> search(Specification<ReservationEntity> spec, Pageable pageable);

  Optional<Reservation> findById(UUID id);

  Reservation findByIdOrElseThrow(UUID id);

  Page<Reservation> findReservationsByUserId(UUID userId, Pageable pageable);

  Reservation findReservationByIdAndUserIdOrElseThrow(UUID reservationId, UUID userId);

  List<Reservation> findAllFutureRecurringReservations(UUID recurringReservationId);

  List<Reservation> findAllFutureReservationsByIds(List<UUID> ids);

  List<Reservation> findAllPastReservationsByUser(UUID userId);

  List<Reservation> findAllFutureActiveReservationsByUser(UUID userId);
}
