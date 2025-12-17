package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ScheduleEntryEntity;
import java.time.LocalDate;
import java.time.LocalTime;
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
   * Busca todos os agendamentos de uma data específica.
   * Retorna todos os tipos de ScheduleEntry (Reservation, Training, BlockedTime).
   *
   *
   * @param date data do agendamento
   * @return lista de todos os agendamentos da data, ordenados por horário de início
   */
  @Query(
      """
      SELECT s FROM ScheduleEntryEntity s
      WHERE s.dateTimeSlot.date = :date
      ORDER BY s.dateTimeSlot.timeInterval.startTime ASC
      """)
  List<ScheduleEntryEntity> findAllSchedulesByDate(@Param("date") LocalDate date);

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

  /**
   * Busca todas as reservas confirmadas cujo horário de término é posterior ao momento especificado.
   * Utilizado para reagendar conclusão automática de reservas quando a aplicação é reiniciada.
   *
   * <p>Critérios de busca:
   * <ul>
   *   <li>Status = CONFIRMED</li>
   *   <li>Data > data de referência OU (Data = data de referência E Horário de término > hora de referência)</li>
   * </ul>
   *
   * @param date data de referência
   * @param time hora de referência
   * @return lista de reservas confirmadas ordenadas por data e horário de término
   */
  @Query(
      """
      SELECT r FROM ReservationEntity r
      WHERE r.status = 'CONFIRMED'
      AND (r.dateTimeSlot.date > :date
           OR (r.dateTimeSlot.date = :date AND r.dateTimeSlot.timeInterval.endTime > :time))
      ORDER BY r.dateTimeSlot.date ASC, r.dateTimeSlot.timeInterval.endTime ASC
      """)
  List<ReservationEntity> findConfirmedReservationsEndedAfter(
      @Param("date") LocalDate date, @Param("time") LocalTime time);
}
