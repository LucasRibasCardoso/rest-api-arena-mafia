package com.projetoExtensao.arenaMafia.infrastructure.persistence.entity;

import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.embeddable.DateTimeSlotEmbeddable;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.DiscriminatorOptions;

@Entity
@Table(
    name = "tb_schedule_entries",
    indexes = {
      @Index(name = "idx_schedule_entries_court_id", columnList = "court_id"),
      @Index(name = "idx_schedule_entries_date", columnList = "date"),
      @Index(name = "idx_schedule_entries_court_date", columnList = "court_id, date"),
      @Index(name = "idx_schedule_entries_type", columnList = "entry_type")
    })
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "entry_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)
public abstract class ScheduleEntryEntity {

  @Id private UUID id;

  @Column(name = "court_id", nullable = false)
  private UUID courtId;

  @Embedded private DateTimeSlotEmbeddable dateTimeSlot;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  // Construtor padrão necessário para JPA
  protected ScheduleEntryEntity() {}

  // --- Getters e Setters ---

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getCourtId() {
    return courtId;
  }

  public void setCourtId(UUID courtId) {
    this.courtId = courtId;
  }

  public DateTimeSlot getDateTimeSlot() {
    return dateTimeSlot != null ? dateTimeSlot.toDomain() : null;
  }

  public void setDateTimeSlot(DateTimeSlot dateTimeSlot) {
    this.dateTimeSlot = DateTimeSlotEmbeddable.fromDomain(dateTimeSlot);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
