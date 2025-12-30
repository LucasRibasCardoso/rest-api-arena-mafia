package com.projetoExtensao.arenaMafia.application.schedule.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepositoryPort {

  Reservation save(Reservation reservation);

  Optional<Reservation> findById(UUID id);

  Reservation findByIdOrElseThrow(UUID id);

  Page<Reservation> findReservationsByUserId(UUID userId, Pageable pageable);

  Reservation findReservationByIdAndUserIdOrElseThrow(UUID reservationId, UUID userId);

  List<Reservation> findAllConfirmedReservationsWithEndTimeAfter(LocalDateTime dateTime);
}
