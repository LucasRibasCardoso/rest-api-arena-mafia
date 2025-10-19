package com.projetoExtensao.arenaMafia.infrastructure.persistence.embeddable;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;

@Embeddable
public class TimeIntervalEmbeddable {

  @Column(name = "open_time", nullable = false)
  private LocalTime openTime;

  @Column(name = "close_time", nullable = false)
  private LocalTime closeTime;

  protected TimeIntervalEmbeddable() {}

  private TimeIntervalEmbeddable(LocalTime openTime, LocalTime closeTime) {
    this.openTime = openTime;
    this.closeTime = closeTime;
  }

  public static TimeIntervalEmbeddable fromDomain(TimeInterval interval) {
    if (interval == null) {
      return null;
    }
    return new TimeIntervalEmbeddable(interval.openTime(), interval.closeTime());
  }

  public TimeInterval toDomain() {
    return new TimeInterval(openTime, closeTime);
  }

  // Getters para o JPA
  public LocalTime getOpenTime() {
    return openTime;
  }

  public LocalTime getCloseTime() {
    return closeTime;
  }
}
