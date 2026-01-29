package com.projetoExtensao.arenaMafia.application.schedule.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationRepositoryPort {

  Reservation save(Reservation reservation);

  void saveAll(List<Reservation> reservations);

  Optional<Reservation> findById(UUID id);

  Reservation findByIdOrElseThrow(UUID id);

  Page<Reservation> findReservationsByUserId(UUID userId, Pageable pageable);

  Reservation findReservationByIdAndUserIdOrElseThrow(UUID reservationId, UUID userId);

  List<Reservation> findAllFutureRecurringReservations(UUID recurringReservationId);

  List<Reservation> findAllFutureReservationsByIds(List<UUID> ids);
}
