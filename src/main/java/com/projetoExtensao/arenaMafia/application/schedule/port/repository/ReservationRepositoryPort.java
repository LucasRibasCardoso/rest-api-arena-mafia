package com.projetoExtensao.arenaMafia.application.schedule.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ReservationRepositoryPort {

  Reservation save(Reservation reservation);

  Optional<Reservation> findById(UUID id);

  Reservation findByIdOrElseThrow(UUID id);

  List<Reservation> findAllByIds(List<UUID> ids);

  Page<Reservation> findReservationsByUserId(UUID userId, Pageable pageable);

  Reservation findReservationByIdAndUserIdOrElseThrow(UUID reservationId, UUID userId);

  List<Reservation> findAllConfirmedReservationsWithEndTimeAfter(LocalDateTime dateTime);

  List<Reservation> findAllConfirmedReservationsWithEndTimeBeforeOrEqual(LocalDateTime dateTime);

  boolean existsConfirmedReservationsAfter(UUID courtId, LocalDate date);

  boolean existsConfirmedReservationsForDaysAndTime(Set<DayOfWeek> daysOfWeek, LocalTime startTime, LocalTime endTime, LocalDate afterDate);
}
