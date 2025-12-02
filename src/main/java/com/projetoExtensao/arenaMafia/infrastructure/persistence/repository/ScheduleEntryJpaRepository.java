package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ScheduleEntryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  /**
   * Busca todas as reservas de um usuário específico com paginação. Ordena por data e horário de
   * início em ordem decrescente (mais recentes primeiro).
   *
   * @param userId ID do usuário
   * @param pageable informações de paginação e ordenação
   * @return página contendo as reservas do usuário
   */
  @Query(
      """
      SELECT r FROM ReservationEntity r
      WHERE r.userId = :userId
      ORDER BY r.dateTimeSlot.date DESC, r.dateTimeSlot.timeInterval.startTime DESC
      """)
  Page<ReservationEntity> findReservationsByUserId(@Param("userId") UUID userId, Pageable pageable);

  /**
   * Busca uma reserva específica por ID e usuário. Garante que a reserva pertence ao usuário
   * autenticado.
   *
   * @param reservationId ID da reserva
   * @param userId ID do usuário
   * @return a entidade de reserva correspondente
   */
  @Query(
      """
      SELECT r FROM ReservationEntity r
      WHERE r.id = :reservationId
      AND r.userId = :userId
      """)
  Optional<ReservationEntity> findReservationByIdAndUser(
      @Param("reservationId") UUID reservationId, @Param("userId") UUID userId);
}
