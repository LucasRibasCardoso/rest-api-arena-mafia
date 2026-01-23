package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationJpaRepository
    extends JpaRepository<ReservationEntity, UUID>, JpaSpecificationExecutor<ReservationEntity> {

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

}
