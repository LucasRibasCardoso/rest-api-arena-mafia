package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
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
   * Busca todas as reservas recorrentes futuras
   *
   * @param recurringReservationId ID da reserva recorrente
   * @return lista de reservas recorrentes futuras
   */
  @Query(
      """
          SELECT r FROM ReservationEntity r
          WHERE r.recurringReservationId = :recurringReservationId
          AND r.dateTimeSlot.date >= CURRENT_DATE
          AND r.status = 'CONFIRMED'
          ORDER BY r.dateTimeSlot.date ASC, r.dateTimeSlot.timeInterval.startTime ASC
          """)
  List<ReservationEntity> findFutureRecurringReservations(
      @Param("recurringReservationId") UUID recurringReservationId);

  /**
   * Busca todas as reservas futuras com base em uma lista de IDs
   *
   * @param ids Lista de IDs das reservas
   * @return lista de reservas futuras
   */
  @Query(
      """
          SELECT r FROM ReservationEntity r
          WHERE r.id IN :ids
          AND r.dateTimeSlot.date >= CURRENT_DATE
          AND r.status = 'CONFIRMED'
          ORDER BY r.dateTimeSlot.date ASC, r.dateTimeSlot.timeInterval.startTime ASC
          """)
  List<ReservationEntity> findAllFutureReservationsByIds(@Param("ids") List<UUID> ids);

  /**
   * Busca todas as reservas passadas (histórico) de um usuário. Considera datas estritamente
   * anteriores à data atual.
   */
  @Query(
      """
              SELECT r FROM ReservationEntity r
              WHERE r.userId = :userId
              AND r.dateTimeSlot.date < CURRENT_DATE
              ORDER BY r.dateTimeSlot.date DESC, r.dateTimeSlot.timeInterval.startTime DESC
              """)
  List<ReservationEntity> findAllPastReservationsByUser(@Param("userId") UUID userId);

  /**
   * Busca todas as reservas futuras ativas (não canceladas) de um usuário. Considera hoje e datas
   * futuras.
   */
  @Query(
      """
              SELECT r FROM ReservationEntity r
              WHERE r.userId = :userId
              AND r.dateTimeSlot.date >= CURRENT_DATE
              AND r.status <> 'CANCELLED'
              ORDER BY r.dateTimeSlot.date ASC, r.dateTimeSlot.timeInterval.startTime ASC
              """)
  List<ReservationEntity> findAllFutureActiveReservationsByUser(@Param("userId") UUID userId);
}
