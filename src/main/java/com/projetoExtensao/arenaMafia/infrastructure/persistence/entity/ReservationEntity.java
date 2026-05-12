package com.projetoExtensao.arenaMafia.infrastructure.persistence.entity;

import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "tb_reservations",
    indexes = {
      @Index(name = "idx_reservations_user_id", columnList = "user_id"),
      @Index(name = "idx_reservations_modality_id", columnList = "modality_id"),
      @Index(name = "idx_reservations_status", columnList = "status"),
      @Index(name = "idx_reservations_scheduled_by_admin", columnList = "scheduled_by_admin_id"),
      @Index(name = "idx_reservations_recurring", columnList = "recurring_reservation_id")
    })
@DiscriminatorValue("RESERVATION")
public class ReservationEntity extends ScheduleEntryEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private UserEntity user;

  @Column(name = "modality_id", nullable = false)
  private UUID modalityId;

  @Column(name = "scheduled_by_admin_id")
  private UUID scheduledByAdminId;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private ReservationStatus status;

  @Column(name = "recurring_reservation_id")
  private UUID recurringReservationId;

  @Column(name = "cancelled_by_admin_id")
  private UUID cancelledByAdminId;

  // Construtor padrão necessário para JPA
  public ReservationEntity() {}

  // --- Getters e Setters ---

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getModalityId() {
    return modalityId;
  }

  public void setModalityId(UUID modalityId) {
    this.modalityId = modalityId;
  }

  public UUID getScheduledByAdminId() {
    return scheduledByAdminId;
  }

  public void setScheduledByAdminId(UUID scheduledByAdminId) {
    this.scheduledByAdminId = scheduledByAdminId;
  }

  public UUID getCancelledByAdminId() {
    return cancelledByAdminId;
  }

  public void setCancelledByAdminId(UUID cancelledByAdminId) {
    this.cancelledByAdminId = cancelledByAdminId;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public void setStatus(ReservationStatus status) {
    this.status = status;
  }

  public UUID getRecurringReservationId() {
    return recurringReservationId;
  }

  public void setRecurringReservationId(UUID recurringReservationId) {
    this.recurringReservationId = recurringReservationId;
  }
}
