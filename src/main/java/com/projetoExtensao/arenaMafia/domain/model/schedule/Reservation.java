package com.projetoExtensao.arenaMafia.domain.model.schedule;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidReservationException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ReservationStatusConflictException;
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
  private ReservationStatus status;
  private final UUID recurringReservationId;

  /**
   * Factory method para criar uma reserva feita pelo próprio usuário. Uso: quando o usuário
   * autenticado faz uma reserva para si mesmo.
   *
   * @param modalityId id da modalidade, é obrigatório
   * @param courtId id da quadra, é obrigatório
   * @param userId id do usuário que está fazendo a reserva, é obrigatório
   * @param price preço da reserva, é obrigatório e deve ser maior ou igual a zero
   * @param dateTimeSlot slot de data e hora da reserva, é obrigatório
   * @return uma nova instância de Reservation
   */
  public static Reservation createByUser(
      UUID modalityId, UUID courtId, UUID userId, BigDecimal price, DateTimeSlot dateTimeSlot) {

    UUID id = UUID.randomUUID();
    Instant createdAt = Instant.now();
    ReservationStatus status = ReservationStatus.CONFIRMED;

    return new Reservation(
        id,
        courtId,
        modalityId,
        userId,
        null, // scheduledByAdminId é null quando o próprio usuário faz a reserva
        price,
        dateTimeSlot,
        status,
        null, // recurringReservationId é null para reservas individuais
        createdAt);
  }

  /**
   * Factory method para criar uma reserva feita por um administrador em nome de um usuário. Uso:
   * quando um admin agenda uma reserva para outro usuário.
   *
   * @param modalityId id da modalidade, é obrigatório
   * @param courtId id da quadra, é obrigatório
   * @param userId id do usuário para quem a reserva está sendo feita, é obrigatório
   * @param scheduledByAdminId id do admin que está criando a reserva, é obrigatório
   * @param price preço da reserva, é obrigatório e deve ser maior ou igual a zero
   * @param dateTimeSlot slot de data e hora da reserva, é obrigatório
   * @return uma nova instância de Reservation
   */
  public static Reservation createByAdmin(
      UUID modalityId,
      UUID courtId,
      UUID userId,
      UUID scheduledByAdminId,
      BigDecimal price,
      DateTimeSlot dateTimeSlot) {

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
        null, // recurringReservationId é null para reservas individuais
        createdAt);
  }

  /**
   * Factory method para criar uma reserva recorrente. Uso: quando um admin cria uma reserva que se
   * repete periodicamente.
   *
   * @param modalityId id da modalidade, é obrigatório
   * @param courtId id da quadra, é obrigatório
   * @param userId id do usuário para quem a reserva está sendo feita, é obrigatório
   * @param scheduledByAdminId id do admin que está criando a reserva recorrente, é obrigatório
   * @param price preço da reserva, é obrigatório e deve ser maior ou igual a zero
   * @param dateTimeSlot slot de data e hora da reserva, é obrigatório
   * @param recurringReservationId id que agrupa todas as reservas recorrentes, é obrigatório
   * @return uma nova instância de Reservation
   */
  public static Reservation createRecurring(
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

  // --- Comportamentos de Negócio ---

  public void cancel() {
    if (this.status == ReservationStatus.CANCELLED) {
      throw new ReservationStatusConflictException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
    }

    if (this.status == ReservationStatus.COMPLETED) {
      throw new ReservationStatusConflictException(ErrorCode.RESERVATION_ALREADY_COMPLETED);
    }

    this.status = ReservationStatus.CANCELLED;
  }

  public void complete() {
    if (this.status == ReservationStatus.CANCELLED) {
      throw new ReservationStatusConflictException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
    }

    if (this.status == ReservationStatus.COMPLETED) {
      throw new ReservationStatusConflictException(ErrorCode.RESERVATION_ALREADY_COMPLETED);
    }

    this.status = ReservationStatus.COMPLETED;
  }

  public boolean isConfirmed() {
    return this.status == ReservationStatus.CONFIRMED;
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
