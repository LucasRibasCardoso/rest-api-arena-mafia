package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidModalityNameFormatException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Modality {

  private final UUID id;
  private String name;
  private final Instant createdAt;

  /**
   * Factory Method para criar uma nova modalidade.
   *
   * @param name nome da modalidade
   * @return uma nova instância de Modality
   */
  public static Modality create(String name) {
    UUID newId = UUID.randomUUID();
    Instant now = Instant.now();

    return new Modality(newId, name, now);
  }

  /**
   * Factory Method para RECONSTRUIR uma modalidade a partir de dados existentes do banco. Esse
   * método é usado pelo MapStruct para mapear uma entidade para Modality.
   *
   * @param id id da modalidade
   * @param name nome da modalidade
   * @param createdAt data de criação da modalidade
   * @return uma instância de Modality reconstruída
   */
  public static Modality reconstitute(UUID id, String name, Instant createdAt) {
    return new Modality(id, name, createdAt);
  }

  private Modality(UUID id, String name, Instant createdAt) {
    validateName(name);
    this.id = id;
    this.name = name;
    this.createdAt = createdAt;
  }

  // Validações
  public static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new InvalidModalityNameFormatException(ErrorCode.MODALITY_NAME_REQUIRED);
    }

    if (name.length() < 3 || name.length() > 100) {
      throw new InvalidModalityNameFormatException(ErrorCode.MODALITY_NAME_INVALID_LENGTH);
    }
  }

  // Atualizar atributos
  public void updateName(String newName) {
    validateName(newName);
    this.name = newName;
  }

  // Getters
  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Modality modality)) return false;
    return Objects.equals(id, modality.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
