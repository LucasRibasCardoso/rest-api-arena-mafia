package com.projetoExtensao.arenaMafia.infrastructure.persistence.entity;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.embeddable.TimeIntervalEmbeddable;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "tb_price_rules",
    indexes = {
      @Index(name = "idx_price_rules_is_active", columnList = "is_active"),
      @Index(name = "idx_price_rules_is_default", columnList = "is_default"),
      @Index(name = "idx_price_rules_priority", columnList = "priority"),
      @Index(name = "idx_price_rules_start_time", columnList = "start_time"),
      @Index(name = "idx_price_rules_end_time", columnList = "end_time")
    })
public class PriceRuleEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @ElementCollection(targetClass = DayOfWeek.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "tb_price_rule_days", joinColumns = @JoinColumn(name = "price_rule_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", length = 20)
  private Set<DayOfWeek> daysOfWeek;

  @Embedded private TimeIntervalEmbeddable timeInterval;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "priority", nullable = false)
  private int priority;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  // Construtor padrão necessário para JPA
  public PriceRuleEntity() {}

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

  public Set<DayOfWeek> getDaysOfWeek() {
    return this.daysOfWeek;
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

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
