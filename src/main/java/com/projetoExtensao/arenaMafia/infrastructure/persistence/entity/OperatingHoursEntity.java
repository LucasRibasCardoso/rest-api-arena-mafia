package com.projetoExtensao.arenaMafia.infrastructure.persistence.entity;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.embeddable.TimeIntervalEmbeddable;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "tb_operating_hours",
    indexes = {
      @Index(name = "idx_operating_hours_is_active", columnList = "is_active"),
      @Index(name = "idx_operating_hours_start_time", columnList = "start_time"),
      @Index(name = "idx_operating_hours_end_time", columnList = "end_time")
    })
public class OperatingHoursEntity {

  @Id private UUID id;

  @ElementCollection(targetClass = DayOfWeek.class, fetch = FetchType.EAGER)
  @CollectionTable(
      name = "tb_operating_hours_days",
      joinColumns = @JoinColumn(name = "operating_hours_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false, length = 20)
  private Set<DayOfWeek> daysOfWeek;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "startTime", column = @Column(name = "start_time")),
    @AttributeOverride(name = "endTime", column = @Column(name = "end_time"))
  })
  private TimeIntervalEmbeddable timeInterval;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  // Construtor padrão necessário para JPA
  public OperatingHoursEntity() {}

  // --- Getters e Setters ---
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Set<DayOfWeek> getDaysOfWeek() {
    return daysOfWeek;
  }

  public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
    this.daysOfWeek = daysOfWeek;
  }

  public TimeInterval getTimeInterval() {
    return timeInterval != null ? timeInterval.toDomain() : null;
  }

  public void setTimeInterval(TimeInterval timeInterval) {
    this.timeInterval = TimeIntervalEmbeddable.fromDomain(timeInterval);
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
