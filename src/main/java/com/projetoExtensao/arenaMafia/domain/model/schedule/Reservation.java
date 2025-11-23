package com.projetoExtensao.arenaMafia.domain.model.schedule;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidReservationException;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Reservation extends ScheduleEntry {

  private final UUID userId;
  private final UUID modalityId;
  private final UUID scheduledByAdminId;
  private final BigDecimal price;
  private final ReservationStatus status;
  private final UUID recurringReservationId;

  /**
   * Factory method para criar uma reserva nova.
   *
   * @param modalityId id da modalidade, é obrigatório
   * @param courtId id da quadra, é obrigatório
   * @param userId id do usuário que fez a reserva, é obrigatório
   * @param scheduledByAdminId id do admin que fez a reserva pelo usuário, nulo se foi feita pelo
   *     próprio usuário
   * @param price preço da reserva, é obrigatório e deve ser maior ou igual a zero
   * @param dateTimeSlot slot de data e hora da reserva, é obrigatório
   * @param recurringReservationId id da reserva recorrente, pode ser nulo se não for recorrente
   * @return uma nova instância de Reservation
   */
  public static Reservation create(
      UUID modalityId,
      UUID courtId,
      UUID userId,
      UUID scheduledByAdminId,
      BigDecimal price,
      DateTimeSlot dateTimeSlot,
      UUID recurringReservationId) {

    UUID id = UUID.randomUUID();
    Instant createdAt = Instant.now();
    ReservationStatus status = ReservationStatus.CONFIRMED;

    return new Reservation(
        id,
        courtId,
        modalityId,
        userId,
        scheduledByAdminId,
        price,
        dateTimeSlot,
        status,
        recurringReservationId,
        createdAt);
  }

  /**
   * Factory method para reconstituir uma reserva a partir de dados persistidos. Utilizado pelo
   * MapStruct.
   *
   * @param id id da reserva
   * @param courtId id da quadra
   * @param modalityId id da modalidade
   * @param userId id do usuário
   * @param scheduledByAdminId id do admin que agendou
   * @param price preço da reserva
   * @param dateTimeSlot slot de data e hora
   * @param status status da reserva
   * @param recurringReservationId id da reserva recorrente
   * @param createdAt momento de criação da reserva
   * @return uma instância de Reservation
   */
  public static Reservation reconstitute(
      UUID id,
      UUID courtId,
      UUID modalityId,
      UUID userId,
      UUID scheduledByAdminId,
      BigDecimal price,
      DateTimeSlot dateTimeSlot,
      ReservationStatus status,
      UUID recurringReservationId,
      Instant createdAt) {
    return new Reservation(
        id,
        courtId,
        modalityId,
        userId,
        scheduledByAdminId,
        price,
        dateTimeSlot,
        status,
        recurringReservationId,
        createdAt);
  }

  private Reservation(
      UUID id,
      UUID courtId,
      UUID modalityId,
      UUID userId,
      UUID scheduledByAdminId,
      BigDecimal price,
      DateTimeSlot dateTimeSlot,
      ReservationStatus status,
      UUID recurringReservationId,
      Instant createdAt) {

    super(id, courtId, dateTimeSlot, createdAt);

    validateModalityId(modalityId);
    validateUserId(userId);
    validatePrice(price);

    this.userId = userId;
    this.modalityId = modalityId;
    this.scheduledByAdminId = scheduledByAdminId;
    this.price = price;
    this.status = status;
    this.recurringReservationId = recurringReservationId;
  }

  // --- Validações ---
  private static void validateModalityId(UUID modalityId) {
    if (modalityId == null) {
      throw new InvalidReservationException(ErrorCode.RESERVATION_MODALITY_ID_REQUIRED);
    }
  }

  private static void validateUserId(UUID userId) {
    if (userId == null) {
      throw new InvalidReservationException(ErrorCode.RESERVATION_USER_ID_REQUIRED);
    }
  }

  private static void validatePrice(BigDecimal price) {
    if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
      throw new InvalidReservationException(ErrorCode.RESERVATION_PRICE_INVALID);
    }
  }

  // --- Getters ---
  public UUID getUserId() {
    return userId;
  }

  public UUID getModalityId() {
    return modalityId;
  }

  public UUID getScheduledByAdminId() {
    return scheduledByAdminId;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public UUID getRecurringReservationId() {
    return recurringReservationId;
  }
}
