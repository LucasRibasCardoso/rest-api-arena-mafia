package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.OperatingHoursStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class OperatingHours {

  private final UUID id;
  private final DayOfWeek dayOfWeek;
  private final TimeInterval timeInterval;
  private boolean isActive;
  private final Instant createdAt;

  /**
   * Cria uma nova instância de OperatingHours com um ID gerado, ativo por padrão.
   *
   * @param dayOfWeek dia da semana
   * @param timeInterval slot de horário de funcionamento (inicio e fim)
   * @return nova instância de OperatingHours
   */
  public static OperatingHours create(DayOfWeek dayOfWeek, TimeInterval timeInterval) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    boolean isActive = true;
    return new OperatingHours(id, dayOfWeek, timeInterval, isActive, now);
  }

  /**
   * Reconstitui uma instância de OperatingHours a partir dos dados fornecidos. Usado principalmente
   * para reconstruir objetos a partir de dados persistidos ou MapStruct.
   *
   * @param id identificador único
   * @param dayOfWeek dia da semana
   * @param timeInterval slot de horário de funcionamento (inicio e fim)
   * @param isActive indica se o horário de funcionamento está ativo
   * @param createdAt data de criação do horário
   * @return nova instância de OperatingHours
   */
  public static OperatingHours reconstitute(
      UUID id,
      DayOfWeek dayOfWeek,
      TimeInterval timeInterval,
      boolean isActive,
      Instant createdAt) {
    return new OperatingHours(id, dayOfWeek, timeInterval, isActive, createdAt);
  }

  private OperatingHours(
      UUID id,
      DayOfWeek dayOfWeek,
      TimeInterval timeInterval,
      boolean isActive,
      Instant createdAt) {
    validateDayOfWeek(dayOfWeek);
    validateTimeInterval(timeInterval);
    this.id = id;
    this.dayOfWeek = dayOfWeek;
    this.timeInterval = timeInterval;
    this.isActive = isActive;
    this.createdAt = createdAt;
  }

  // --- Validações ---
  public static void validateDayOfWeek(DayOfWeek dayOfWeek) {
    if (dayOfWeek == null) {
      throw new InvalidTimeIntervalException(ErrorCode.DAY_OF_WEEK_REQUIRED);
    }
  }

  public static void validateTimeInterval(TimeInterval timeInterval) {
    if (timeInterval == null) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_REQUIRED);
    }
  }

  // --- Métodos de Negócio ---
  public void disable() {
    if (!this.isActive) {
      throw new OperatingHoursStatusConflictException(ErrorCode.OPERATING_HOURS_ALREADY_DISABLED);
    }
    this.isActive = false;
  }

  public void enable() {
    if (this.isActive) {
      throw new OperatingHoursStatusConflictException(ErrorCode.OPERATING_HOURS_ALREADY_ENABLED);
    }
    this.isActive = true;
  }

  /**
   * Valida se este horário não sobrepõe outro horário do mesmo dia.
   *
   * @param other outro horário de funcionamento
   * @throws InvalidTimeIntervalException se houver sobreposição de horários
   */
  public void validateNoOverlapWithSameDay(OperatingHours other) {
    if (other != null && this.dayOfWeek == other.dayOfWeek) {
      this.timeInterval.validateNoOverlap(other.timeInterval);
    }
  }

  // --- Getters ---
  public UUID getId() {
    return id;
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public TimeInterval getTimeInterval() {
    return timeInterval;
  }

  public boolean isActive() {
    return isActive;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof OperatingHours that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
