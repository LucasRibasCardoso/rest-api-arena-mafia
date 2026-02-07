package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.config.bean.SchedulingConfig;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ScheduleEntryEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleEntryJpaRepository
    extends JpaRepository<ScheduleEntryEntity, UUID>,
        JpaSpecificationExecutor<ScheduleEntryEntity> {

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
  @Query(
      """
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

  @Query(
      """
                SELECT s FROM ScheduleEntryEntity s
                WHERE (TYPE(s) = BlockedTimeEntity
                  OR
                  (TYPE(s) = ReservationEntity AND TREAT(s AS ReservationEntity).status = 'CONFIRMED'))
                AND (s.dateTimeSlot.date < :date
                     OR (s.dateTimeSlot.date = :date AND s.dateTimeSlot.timeInterval.endTime <= :time))
                ORDER BY s.dateTimeSlot.date ASC, s.dateTimeSlot.timeInterval.endTime ASC
                """)
  List<ScheduleEntryEntity> findAllActiveSchedulesEndedBeforeOrEqual(@Param("date") LocalDate date, @Param("time") LocalTime time);

  @Query(
      """
              SELECT s FROM ScheduleEntryEntity s
              WHERE (TYPE(s) = BlockedTimeEntity
                  OR
                  (TYPE(s) = ReservationEntity AND TREAT(s AS ReservationEntity).status = 'CONFIRMED'))
              AND (s.dateTimeSlot.date > :date
                   OR (s.dateTimeSlot.date = :date AND s.dateTimeSlot.timeInterval.endTime > :time))
              ORDER BY s.dateTimeSlot.date ASC, s.dateTimeSlot.timeInterval.endTime ASC
              """)
  List<ScheduleEntryEntity> findAllActiveSchedulesEndedAfter(@Param("date") LocalDate date, @Param("time") LocalTime time);
}
