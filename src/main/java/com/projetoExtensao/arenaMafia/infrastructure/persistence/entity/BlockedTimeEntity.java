package com.projetoExtensao.arenaMafia.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "tb_blocked_times",
    indexes = {
      @Index(name = "idx_blocked_times_admin", columnList = "blocked_by_admin_id"),
      @Index(name = "idx_blocked_times_recurring", columnList = "recurring_blocked_time_id")
    })
@DiscriminatorValue("BLOCKED_TIME")
public class BlockedTimeEntity extends ScheduleEntryEntity {

  @Column(name = "description", nullable = false, length = 500)
  private String description;

  @Column(name = "blocked_by_admin_id", nullable = false)
  private UUID blockedByAdminId;

  @Column(name = "is_full_day", nullable = false)
  private Boolean isFullDay;

  @Column(name = "recurring_blocked_time_id")
  private UUID recurringBlockedTimeId;

  // Construtor padrão necessário para JPA
  public BlockedTimeEntity() {}

  // --- Getters e Setters ---

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public UUID getBlockedByAdminId() {
    return blockedByAdminId;
  }

  public void setBlockedByAdminId(UUID blockedByAdminId) {
    this.blockedByAdminId = blockedByAdminId;
  }

  public Boolean isFullDay() {
    return isFullDay;
  }

  public void setIsFullDay(Boolean isFullDay) {
    this.isFullDay = isFullDay;
  }

  public UUID getRecurringBlockedTimeId() {
    return recurringBlockedTimeId;
  }

  public void setRecurringBlockedTimeId(UUID recurringBlockedTimeId) {
    this.recurringBlockedTimeId = recurringBlockedTimeId;
  }
}
