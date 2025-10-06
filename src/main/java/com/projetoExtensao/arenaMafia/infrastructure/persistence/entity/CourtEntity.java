package com.projetoExtensao.arenaMafia.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "tb_courts",
    indexes = {
      @Index(name = "idx_courts_name", columnList = "name"),
      @Index(name = "idx_courts_is_active", columnList = "is_active"),
      @Index(name = "idx_courts_offset_minutes", columnList = "offset_minutes")
    })
public class CourtEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "offset_minutes", nullable = false)
  private Integer offsetMinutes;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @ManyToMany
  @JoinTable(
      name = "tb_court_modalities",
      joinColumns = @JoinColumn(name = "court_id"),
      inverseJoinColumns = @JoinColumn(name = "modality_id"))
  private Set<ModalityEntity> modalities = new HashSet<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  // Construtor padrão necessário para JPA
  public CourtEntity() {}

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getOffsetMinutes() {
    return offsetMinutes;
  }

  public void setOffsetMinutes(Integer offsetMinutes) {
    this.offsetMinutes = offsetMinutes;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public Set<ModalityEntity> getModalities() {
    return modalities;
  }

  public void setModalities(Set<ModalityEntity> modalities) {
    this.modalities = modalities;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
