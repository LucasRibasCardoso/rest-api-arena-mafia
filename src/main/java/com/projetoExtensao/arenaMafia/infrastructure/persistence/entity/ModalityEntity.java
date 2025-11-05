package com.projetoExtensao.arenaMafia.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "tb_modalities",
    indexes = {
      @Index(name = "idx_modalities_name", columnList = "name"),
    })
public class ModalityEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
  private boolean isActive;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  // Construtor padrão necessário para JPA
  public ModalityEntity() {}

  // --- Getters e Setters ---
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
