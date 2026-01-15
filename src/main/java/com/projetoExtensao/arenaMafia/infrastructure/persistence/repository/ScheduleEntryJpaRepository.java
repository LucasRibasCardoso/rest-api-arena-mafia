package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.BlockedTimeEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ScheduleEntryEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleEntryJpaRepository extends JpaRepository<ScheduleEntryEntity, UUID> {

  // ==================== QUERIES GENÉRICAS (ScheduleEntry) ====================

  /**
   * Busca todos os agendamentos de uma data específica. Retorna todos os tipos de ScheduleEntry
   * (Reservation, BlockedTime).
   *
   * @param date data do agendamento
   * @return lista de todos os agendamentos da data, ordenados por horário de início
   */
  @Query(
      """
      SELECT s FROM ScheduleEntryEntity s
      WHERE s.dateTimeSlot.date = :date
      AND (TYPE(s) = BlockedTimeEntity
          OR
          (TYPE(s) = ReservationEntity AND TREAT(s AS ReservationEntity).status = 'CONFIRMED'))
      ORDER BY s.dateTimeSlot.timeInterval.startTime ASC
      """)
  List<ScheduleEntryEntity> findAllSchedulesByDate(@Param("date") LocalDate date);

  /**
   * Buscar todos os agendamentos ativos (Reservations e BlockedTimes) para uma quadra específica
   * após uma data determinada.
   *
   * @param courtId ID da quadra
   * @return lista de agendamentos ativos ordenados por data e horário de início
   */
  @Query(
      """
       SELECT s FROM ScheduleEntryEntity  s
       WHERE s.courtId = :courtId
       AND (TYPE(s) = BlockedTimeEntity
          OR
          (TYPE(s) = ReservationEntity AND TREAT(s AS ReservationEntity).status = 'CONFIRMED'))
        AND s.dateTimeSlot.date >= CURRENT_DATE
        ORDER BY s.dateTimeSlot.date ASC, s.dateTimeSlot.timeInterval.startTime ASC
       """)
  List<ScheduleEntryEntity> findAllSchedulesByCourtIdFromToday(@Param("courtId") UUID courtId);

  /**
   * Buscar conflitos (Reservations confirmadas e BlockedTimes) com filtro opcional de dias da
   * semana.
   *
   * <p>Se selectedDaysOfWeek for null ou vazio, retorna conflitos para todos os dias. Caso
   * contrário, filtra apenas os dias da semana especificados.
   *
   * <p><b>Nota:</b> Esta query NÃO valida sobreposição de TimeInterval. A validação de sobreposição
   * deve ser feita no adapter usando o metodo overlaps do TimeInterval, pois essa lógica já
   * considera intervalos que atravessam a meia-noite.
   *
   * @param courtIds IDs das quadras
   * @param startDate data inicial (inclusive)
   * @param endDate data final (inclusive)
   * @param valuesDaysOfWeek conjunto de inteiros representando os dias da semana no formato
   *     PostgreSQL (0=Sunday, 1=Monday, ..., 6=Saturday)
   * @return lista de agendamentos ativos no intervalo de datas e dias da semana especificados
   */
  @Query(
      """
      SELECT s FROM ScheduleEntryEntity s
      WHERE s.courtId IN :courtIds
      AND s.dateTimeSlot.date BETWEEN :startDate AND :endDate
      AND (TYPE(s) = BlockedTimeEntity
          OR
          (TYPE(s) = ReservationEntity AND TREAT(s AS ReservationEntity).status = 'CONFIRMED'))
      AND (:valuesDaysOfWeek IS NULL
          OR FUNCTION('date_part', 'dow', s.dateTimeSlot.date) IN :valuesDaysOfWeek)
      ORDER BY s.dateTimeSlot.date ASC, s.dateTimeSlot.timeInterval.startTime ASC
      """)
  List<ScheduleEntryEntity> findConflictingSchedules(
      @Param("courtIds") List<UUID> courtIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("valuesDaysOfWeek") Set<Integer> valuesDaysOfWeek);

  /**
   * Buscar todos os agendamentos ativos (Reservations confirmadas e BlockedTimes) a partir de hoje,
   * filtrando por dias da semana e intervalo de horário.
   *
   * @param indexDaysOfWeek Conjunto de inteiros representando os dias da semana
   * @param timeIntervalStartTime Horário inicial do escopo a ser verificado
   * @param timeIntervalEndTime Horário final do escopo a ser verificado
   * @return true se existir conflito, false caso contrário
   */
  @Query("""
  SELECT s FROM ScheduleEntryEntity s
  WHERE s.dateTimeSlot.date >= CURRENT_DATE
  AND (TYPE(s) = BlockedTimeEntity
       OR (TYPE(s) = ReservationEntity AND TREAT(s AS ReservationEntity).status = 'CONFIRMED'))
  AND FUNCTION('date_part', 'dow', s.dateTimeSlot.date) IN :indexDaysOfWeek
  AND (
      (s.dateTimeSlot.timeInterval.startTime < s.dateTimeSlot.timeInterval.endTime
       AND (
            s.dateTimeSlot.timeInterval.startTime < :timeIntervalEndTime
            AND
            s.dateTimeSlot.timeInterval.endTime > :timeIntervalStartTime
       )
      )
      OR
      (s.dateTimeSlot.timeInterval.startTime > s.dateTimeSlot.timeInterval.endTime
       AND (
            s.dateTimeSlot.timeInterval.startTime < :timeIntervalEndTime
            OR
            s.dateTimeSlot.timeInterval.endTime > :timeIntervalStartTime
       )
      )
  )
  """)
  List<ScheduleEntryEntity> findSchedulesFromTodayByDaysOfWeekAndTimeInterval(
      @Param("indexDaysOfWeek") Set<Integer> indexDaysOfWeek,
      @Param("timeIntervalStartTime") LocalTime timeIntervalStartTime,
      @Param("timeIntervalEndTime") LocalTime timeIntervalEndTime);

  // ==================== QUERIES ESPECÍFICAS DE RESERVATION ====================

  /**
   * Buscar todos os agendamentos (Reservations e BlockedTimes) ativos para uma quadra numa data
   * específica.
   *
   * @param courtId ID da quadra
   * @param date data do agendamento
   * @return lista de agendamentos ordenados por horário de início
   */
  @Query(
      """
      SELECT s FROM ScheduleEntryEntity s
      WHERE s.courtId = :courtId
      AND s.dateTimeSlot.date = :date
      AND (TYPE(s) = BlockedTimeEntity
          OR
          (TYPE(s) = ReservationEntity AND TREAT(s AS ReservationEntity).status = 'CONFIRMED'))
      ORDER BY s.dateTimeSlot.timeInterval.startTime ASC
      """)
  List<ScheduleEntryEntity> findSchedulesByCourtAndDate(
      @Param("courtId") UUID courtId, @Param("date") LocalDate date);

  /**
   * Buscar todas as reservas de um usuário específico com paginação. Ordena por data e horário de
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
   * Buscar uma reserva específica por ID e usuário. Garante que a reserva pertence ao usuário
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
   * Buscar todas as reservas confirmadas cujo horário de término é posterior ao momento
   * especificado. Utilizado para reagendar conclusão automática de reservas quando a aplicação é
   * reiniciada.
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

  /**
   * Buscar todas as reservas confirmadas cujo horário de término é anterior ou igual ao momento
   * especificado. Utilizado para completar reservas expiradas quando a aplicação é reiniciada.
   *
   * @param date data de referência
   * @param time hora de referência
   * @return lista de reservas confirmadas ordenadas por data e horário de término
   */
  @Query(
      """
        SELECT r FROM ReservationEntity r
        WHERE r.status = 'CONFIRMED'
        AND (r.dateTimeSlot.date < :date
             OR (r.dateTimeSlot.date = :date AND r.dateTimeSlot.timeInterval.endTime <= :time))
        ORDER BY r.dateTimeSlot.date ASC, r.dateTimeSlot.timeInterval.endTime ASC
        """)
  List<ReservationEntity> findConfirmedReservationsEndedBeforeOrEqual(
      @Param("date") LocalDate date, @Param("time") LocalTime time);

  // ==================== QUERIES ESPECÍFICAS DE BLOCKED TIME ====================

  /**
   * Buscar todos os bloqueios que fazem parte de um grupo recorrente.
   *
   * @param recurringBlockedTimeId identificador do grupo de bloqueios recorrentes
   * @return lista de bloqueios do grupo
   */
  @Query(
      """
      SELECT bt FROM BlockedTimeEntity bt
      WHERE bt.recurringBlockedTimeId = :recurringBlockedTimeId
      ORDER BY bt.dateTimeSlot.date ASC, bt.dateTimeSlot.timeInterval.startTime ASC
      """)
  List<BlockedTimeEntity> findAllByRecurringBlockedTimeId(
      @Param("recurringBlockedTimeId") UUID recurringBlockedTimeId);

  /**
   * Buscar todos os bloqueios de uma quadra num intervalo de datas.
   *
   * @param courtId identificador da quadra
   * @param startDate data inicial (inclusive)
   * @param endDate data final (inclusive)
   * @return lista de bloqueios encontrados ordenados por data e horário
   */
  @Query(
      """
      SELECT bt FROM BlockedTimeEntity bt
      WHERE bt.courtId = :courtId
      AND bt.dateTimeSlot.date BETWEEN :startDate AND :endDate
      ORDER BY bt.dateTimeSlot.date ASC, bt.dateTimeSlot.timeInterval.startTime ASC
      """)
  List<BlockedTimeEntity> findByCourtIdAndDateRange(
      @Param("courtId") UUID courtId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * Buscar bloqueios de múltiplas quadras num intervalo de datas.
   *
   * @param courtIds lista de identificadores de quadras
   * @param startDate data inicial (inclusive)
   * @param endDate data final (inclusive)
   * @return lista de bloqueios encontrados ordenados por quadra, data e horário
   */
  @Query(
      """
      SELECT bt FROM BlockedTimeEntity bt
      WHERE bt.courtId IN :courtIds
      AND bt.dateTimeSlot.date BETWEEN :startDate AND :endDate
      ORDER BY bt.courtId ASC, bt.dateTimeSlot.date ASC, bt.dateTimeSlot.timeInterval.startTime ASC
      """)
  List<BlockedTimeEntity> findByCourtIdsAndDateRange(
      @Param("courtIds") List<UUID> courtIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * Deletar todos os bloqueios de um grupo recorrente.
   *
   * @param recurringBlockedTimeId identificador do grupo de bloqueios recorrentes
   */
  @Modifying
  @Query(
      """
      DELETE FROM BlockedTimeEntity bt
      WHERE bt.recurringBlockedTimeId = :recurringBlockedTimeId
      """)
  void deleteByRecurringBlockedTimeId(@Param("recurringBlockedTimeId") UUID recurringBlockedTimeId);

  /**
   * Verificar se existe algum bloqueio ativo para numa data específica.
   *
   * @param courtId identificador da quadra
   * @param date data a verificar
   * @return true se existir bloqueio ativo, false caso contrário
   */
  @Query(
      """
      SELECT COUNT(bt) > 0 FROM BlockedTimeEntity bt
      WHERE bt.courtId = :courtId
      AND bt.dateTimeSlot.date = :date
      """)
  boolean existsByCourtIdAndDate(@Param("courtId") UUID courtId, @Param("date") LocalDate date);
}
