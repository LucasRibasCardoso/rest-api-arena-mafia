package com.projetoExtensao.arenaMafia.infrastructure.persistence.embeddable;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;

@Embeddable
public class TimeIntervalEmbeddable {

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  protected TimeIntervalEmbeddable() {}

  private TimeIntervalEmbeddable(LocalTime startTime, LocalTime endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public static TimeIntervalEmbeddable fromDomain(TimeInterval interval) {
    if (interval == null) {
      return null;
    }
    return new TimeIntervalEmbeddable(interval.startTime(), interval.endTime());
  }

  public TimeInterval toDomain() {
    return new TimeInterval(startTime, endTime);
  }

  // Getters para o JPA
  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }
}
