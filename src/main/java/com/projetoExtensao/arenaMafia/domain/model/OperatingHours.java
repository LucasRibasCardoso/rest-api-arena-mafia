package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.OperatingHoursStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class OperatingHours {

  private final UUID id;
  private final Set<DayOfWeek> daysOfWeek;
  private final TimeInterval timeInterval;
  private boolean isActive;
  private final Instant createdAt;

  /**
   * Cria uma nova instância de OperatingHours com um ID gerado, ativo por padrão.
   *
   * @param daysOfWeek dias da semana aplicáveis
   * @param timeInterval slot de horário de funcionamento (inicio e fim)
   * @return nova instância de OperatingHours
   */
  public static OperatingHours create(Set<DayOfWeek> daysOfWeek, TimeInterval timeInterval) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    boolean isActive = true;

    Set<DayOfWeek> normalizedDaysOfWeek =
        (daysOfWeek != null && daysOfWeek.isEmpty()) ? null : daysOfWeek;

    return new OperatingHours(id, normalizedDaysOfWeek, timeInterval, isActive, now);
  }

  /**
   * Reconstitui uma instância de OperatingHours a partir dos dados fornecidos. Usado principalmente
   * para reconstruir objetos a partir de dados persistidos ou MapStruct.
   *
   * @param id identificador único
   * @param daysOfWeek dias da semana aplicáveis
   * @param timeInterval slot de horário de funcionamento (inicio e fim)
   * @param isActive indica se o horário de funcionamento está ativo
   * @param createdAt data de criação do horário
   * @return nova instância de OperatingHours
   */
  public static OperatingHours reconstitute(
      UUID id,
      Set<DayOfWeek> daysOfWeek,
      TimeInterval timeInterval,
      boolean isActive,
      Instant createdAt) {
    return new OperatingHours(id, daysOfWeek, timeInterval, isActive, createdAt);
  }

  private OperatingHours(
      UUID id,
      Set<DayOfWeek> daysOfWeek,
      TimeInterval timeInterval,
      boolean isActive,
      Instant createdAt) {
    validateTimeInterval(timeInterval);
    this.id = id;
    this.daysOfWeek = daysOfWeek;
    this.timeInterval = timeInterval;
    this.isActive = isActive;
    this.createdAt = createdAt;
  }

  // --- Validações ---

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
   * Valida se este horário não sobrepõe outro horário nos mesmos dias da semana.
   *
   * @param other outro horário de funcionamento
   * @throws InvalidTimeIntervalException se houver sobreposição de horários nos mesmos dias
   */
  public void validateNoOverlapWithSameDay(OperatingHours other) {
    if (other != null && hasCommonDay(other)) {
      this.timeInterval.validateNoOverlapWith(other.timeInterval);
    }
  }

  /**
   * Verifica se há interseção de dias da semana entre este horário e outro.
   *
   * @param other outro horário de funcionamento
   * @return true se há pelo menos um dia em comum
   */
  private boolean hasCommonDay(OperatingHours other) {
    if (this.daysOfWeek == null || other.daysOfWeek == null) {
      return true;
    }
    return this.daysOfWeek.stream().anyMatch(other.daysOfWeek::contains);
  }

  // --- Getters ---
  public UUID getId() {
    return id;
  }

  public Set<DayOfWeek> getDaysOfWeek() {
    return daysOfWeek;
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
