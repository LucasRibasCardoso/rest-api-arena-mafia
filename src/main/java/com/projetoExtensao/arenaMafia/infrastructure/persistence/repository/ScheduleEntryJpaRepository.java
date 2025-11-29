package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ScheduleEntryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleEntryJpaRepository extends JpaRepository<ScheduleEntryEntity, UUID> {

  /**
   * Busca todas as reservas confirmadas para uma quadra específica em uma data específica. Filtra
   * automaticamente apenas reservas com status CONFIRMED.
   *
   * @param courtId ID da quadra
   * @param date data do agendamento
   * @return lista de reservas confirmadas ordenadas por horário de início
   */
  @Query(
      """
      SELECT r FROM ReservationEntity r
      WHERE r.courtId = :courtId
      AND r.dateTimeSlot.date = :date
      AND r.status = 'CONFIRMED'
      ORDER BY r.dateTimeSlot.timeInterval.startTime ASC
      """)
  List<ScheduleEntryEntity> findConfirmedReservationsByCourtAndDate(
      @Param("courtId") UUID courtId, @Param("date") LocalDate date);
}
