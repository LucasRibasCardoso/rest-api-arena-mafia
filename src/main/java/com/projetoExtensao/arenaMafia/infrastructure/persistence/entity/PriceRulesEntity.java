package com.projetoExtensao.arenaMafia.infrastructure.persistence.entity;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
    name = "tb_price_rules",
    indexes = {
      @Index(name = "idx_price_rules_day_of_week", columnList = "day_of_week"),
      @Index(name = "idx_price_rules_is_active", columnList = "is_active"),
      @Index(name = "idx_price_rules_is_default", columnList = "is_default"),
      @Index(name = "idx_price_rules_priority", columnList = "priority"),
      @Index(name = "idx_price_rules_start_time", columnList = "start_time"),
      @Index(name = "idx_price_rules_end_time", columnList = "end_time"),
      @Index(name = "idx_price_rules_unique_default", columnList = "is_default")
    })
public class PriceRulesEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", length = 10)
  private DayOfWeek dayOfWeek;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Column(name = "priority", nullable = false)
  private int priority;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  // Construtor padrão necessário para JPA
  public PriceRulesEntity() {}

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

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(DayOfWeek dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
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

  public void setActive(boolean active) {
    isActive = active;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean aDefault) {
    isDefault = aDefault;
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
