package com.projetoExtensao.arenaMafia.infrastructure.persistence.embeddable;

import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;

@Embeddable
public class DateTimeSlotEmbeddable {

  @Column(nullable = false)
  private LocalDate date;

  @Embedded private TimeIntervalEmbeddable timeInterval;

  protected DateTimeSlotEmbeddable() {}

  private DateTimeSlotEmbeddable(LocalDate date, TimeIntervalEmbeddable timeInterval) {
    this.date = date;
    this.timeInterval = timeInterval;
  }

  public static DateTimeSlotEmbeddable fromDomain(DateTimeSlot dateTimeSlot) {
    if (dateTimeSlot == null) {
      return null;
    }
    var intervalEmbeddable = TimeIntervalEmbeddable.fromDomain(dateTimeSlot.timeInterval());
    return new DateTimeSlotEmbeddable(dateTimeSlot.date(), intervalEmbeddable);
  }

  public DateTimeSlot toDomain() {
    TimeInterval interval = timeInterval.toDomain();
    return new DateTimeSlot(date, interval);
  }

  // --- Getters para o JPA ---
  public LocalDate getDate() {
    return date;
  }

  public TimeIntervalEmbeddable getTimeInterval() {
    return timeInterval;
  }
}
