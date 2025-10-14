package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.CourtModalityRequiredException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.CourtOffsetRequiredException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidCourtNameException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Court {

  private final UUID id;
  private String name;
  private String description;
  private OffsetMinutes offsetMinutes;
  private boolean isActive;
  private Set<UUID> modalityIds;
  private final Instant createdAt;

  /**
   * Cria uma nova instância de Court com um ID gerado e a data de criação atual. Por padrão, uma
   * nova quadra é criada no estado ativo.
   *
   * @param name nome da quadra
   * @param description descrição da quadra
   * @param offsetMinutes horário de inicio (ZERO = 0), (THIRTY = 30)
   * @param modalityIds IDs das modalidades disponíveis na quadra
   * @return uma nova instância de Court
   */
  public static Court create(
      String name, String description, OffsetMinutes offsetMinutes, Set<UUID> modalityIds) {

    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    boolean isActive = true;
    return new Court(id, name, description, offsetMinutes, isActive, modalityIds, now);
  }

  /**
   * Reconstitui uma instância de Court a partir dos dados fornecidos. Usado principalmente para
   * reconstruir objetos a partir de dados persistidos ou MapStruct.
   *
   * @param id ID da quadra
   * @param name nome da quadra
   * @param description descrição da quadra
   * @param offsetMinutes horário de inicio (ZERO = 0), (THIRTY = 30)
   * @param isActive indica se a quadra está ativa
   * @param modalityIds IDs das modalidades disponíveis na quadra
   * @param createdAt data de criação da quadra
   * @return uma nova instância de Court a partir dos dados fornecidos
   */
  public static Court reconstitute(
      UUID id,
      String name,
      String description,
      OffsetMinutes offsetMinutes,
      boolean isActive,
      Set<UUID> modalityIds,
      Instant createdAt) {
    return new Court(id, name, description, offsetMinutes, isActive, modalityIds, createdAt);
  }

  private Court(
      UUID id,
      String name,
      String description,
      OffsetMinutes offsetMinutes,
      boolean isActive,
      Set<UUID> modalityIds,
      Instant createdAt) {

    validateName(name);
    validateModalityIds(modalityIds);
    validateOffsetMinutes(offsetMinutes);
    this.id = id;
    this.name = name;
    this.description = description;
    this.offsetMinutes = offsetMinutes;
    this.isActive = isActive;
    this.modalityIds = modalityIds != null ? new HashSet<>(modalityIds) : new HashSet<>();
    this.createdAt = createdAt;
  }

  // --- Validações ---
  public static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new InvalidCourtNameException(ErrorCode.COURT_NAME_REQUIRED);
    }
    if (name.length() < 3 || name.length() > 100) {
      throw new InvalidCourtNameException(ErrorCode.COURT_NAME_INVALID_LENGTH);
    }
  }

  public static void validateModalityIds(Set<UUID> modalityIds) {
    if (modalityIds == null || modalityIds.isEmpty()) {
      throw new CourtModalityRequiredException();
    }
  }

  public static void validateOffsetMinutes(OffsetMinutes offsetMinutes) {
    if (offsetMinutes == null) {
      throw new CourtOffsetRequiredException();
    }
  }

  // --- Atualizações ---
  public void updateName(String name) {
    if (name != null) {
      validateName(name);
      this.name = name;
    }
  }

  public void updateDescription(String description) {
    if (description != null) {
      this.description = description;
    }
  }

  public void updateOffsetMinutes(OffsetMinutes offsetMinutes) {
    if (offsetMinutes != null) {
      validateOffsetMinutes(offsetMinutes);
      this.offsetMinutes = offsetMinutes;
    }
  }

  public void replaceModalityIds(Set<UUID> newModalityIds) {
    validateModalityIds(newModalityIds);
    this.modalityIds = new HashSet<>(newModalityIds);
  }

  public void disable() {
    if (!this.isActive) {
      throw new CourtStatusConflictException(ErrorCode.COURT_ALREADY_DISABLED);
    }
    this.isActive = false;
  }

  public void enable() {
    if (this.isActive) {
      throw new CourtStatusConflictException(ErrorCode.COURT_ALREADY_ENABLED);
    }
    this.isActive = true;
  }

  // --- Getters ---
  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public OffsetMinutes getOffsetMinutes() {
    return offsetMinutes;
  }

  public boolean isActive() {
    return isActive;
  }

  public Set<UUID> getModalityIds() {
    return Collections.unmodifiableSet(modalityIds);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Court court)) return false;
    return Objects.equals(id, court.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
