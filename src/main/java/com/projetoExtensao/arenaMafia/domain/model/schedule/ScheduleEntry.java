package com.projetoExtensao.arenaMafia.domain.model.schedule;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidScheduleEntryException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public abstract class ScheduleEntry {

  private final UUID id;
  private final UUID courtId;
  private final DateTimeSlot dateTimeSlot;
  private final Instant createdAt;

  /**
   * Construtor protegido para ser usado pelas subclasses.
   *
   * @param id identificador único da entrada no calendário
   * @param courtId identificador da quadra
   * @param dateTimeSlot slot de data e hora do agendamento
   * @param createdAt momento de criação do registro
   */
  protected ScheduleEntry(UUID id, UUID courtId, DateTimeSlot dateTimeSlot, Instant createdAt) {

    validateId(id);
    validateCourtId(courtId);
    validateDateTimeSlot(dateTimeSlot);
    validateCreatedAt(createdAt);

    this.id = id;
    this.courtId = courtId;
    this.dateTimeSlot = dateTimeSlot;
    this.createdAt = createdAt;
  }

  // --- Validações ---

  private void validateId(UUID id) {
    if (id == null) {
      throw new InvalidScheduleEntryException(ErrorCode.SCHEDULE_ENTRY_ID_REQUIRED);
    }
  }

  private void validateCourtId(UUID courtId) {
    if (courtId == null) {
      throw new InvalidScheduleEntryException(ErrorCode.SCHEDULE_ENTRY_COURT_ID_REQUIRED);
    }
  }

  private void validateDateTimeSlot(DateTimeSlot dateTimeSlot) {
    if (dateTimeSlot == null) {
      throw new InvalidScheduleEntryException(ErrorCode.SCHEDULE_ENTRY_DATE_TIME_SLOT_REQUIRED);
    }
  }

  private void validateCreatedAt(Instant createdAt) {
    if (createdAt == null) {
      throw new InvalidScheduleEntryException(ErrorCode.SCHEDULE_ENTRY_CREATED_AT_REQUIRED);
    }
  }

  /**
   * Verifica se o agendamento está ativo. - Para Reservations: verifica o status ativo - Para
   * outros tipos: considera sempre ativo
   *
   * @return true se o agendamento estiver ativo, false caso contrário
   */
  public abstract boolean isActive();

  /**
   * Verifica se este agendamento pertence à modalidade especificada. Cada tipo de ScheduleEntry
   * implementa sua própria lógica.
   *
   * @param modalityId ID da modalidade para verificar
   * @return true se pertence à modalidade, false caso contrário
   */
  public abstract boolean belongsToModality(UUID modalityId);

  // --- Getters ---

  public UUID getId() {
    return id;
  }

  public UUID getCourtId() {
    return courtId;
  }

  public DateTimeSlot getDateTimeSlot() {
    return dateTimeSlot;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  // --- Equals and HashCode ---
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleEntry that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
