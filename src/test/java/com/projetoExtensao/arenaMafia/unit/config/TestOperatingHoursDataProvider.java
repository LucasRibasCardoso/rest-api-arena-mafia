package com.projetoExtensao.arenaMafia.unit.config;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class TestOperatingHoursDataProvider {

  private TestOperatingHoursDataProvider() {}

  public static final Set<DayOfWeek> defaultDaysOfWeek = Set.of(DayOfWeek.MONDAY);
  public static final LocalTime defaultOpenTime = LocalTime.of(8, 0);
  public static final LocalTime defaultCloseTime = LocalTime.of(18, 0);
  public static final TimeInterval defaultTimeInterval =
      new TimeInterval(defaultOpenTime, defaultCloseTime);

  public static OperatingHours createActiveOperatingHours() {
    return OperatingHoursBuilder.defaultOperatingHours().withIsActive(true).build();
  }

  public static OperatingHours createDisabledOperatingHours() {
    return OperatingHoursBuilder.defaultOperatingHours().withIsActive(false).build();
  }

  public static OperatingHours createOperatingHoursForDays(Set<DayOfWeek> daysOfWeek) {
    return OperatingHoursBuilder.defaultOperatingHours().withDaysOfWeek(daysOfWeek).build();
  }

  public static OperatingHours createOperatingHoursWithTimeInterval(TimeInterval timeInterval) {
    return OperatingHoursBuilder.defaultOperatingHours().withTimeInterval(timeInterval).build();
  }

  public static class OperatingHoursBuilder {
    private UUID id = UUID.randomUUID();
    private Set<DayOfWeek> daysOfWeek = defaultDaysOfWeek;
    private TimeInterval timeInterval = defaultTimeInterval;
    private boolean isActive = true;
    private Instant createdAt = Instant.now();

    public static OperatingHoursBuilder defaultOperatingHours() {
      return new OperatingHoursBuilder();
    }

    public OperatingHoursBuilder withId(UUID id) {
      this.id = id;
      return this;
    }

    public OperatingHoursBuilder withDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
      this.daysOfWeek = daysOfWeek;
      return this;
    }

    public OperatingHoursBuilder withTimeInterval(TimeInterval timeInterval) {
      this.timeInterval = timeInterval;
      return this;
    }

    public OperatingHoursBuilder withIsActive(boolean isActive) {
      this.isActive = isActive;
      return this;
    }

    public OperatingHoursBuilder withCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public OperatingHours build() {
      return OperatingHours.reconstitute(id, daysOfWeek, timeInterval, isActive, createdAt);
    }
  }

  public static Stream<Arguments> invalidDaysOfWeekProvider() {
    return Stream.of(
        Arguments.of((Set<DayOfWeek>) null, ErrorCode.DAY_OF_WEEK_REQUIRED),
        Arguments.of(Set.of(), ErrorCode.DAY_OF_WEEK_REQUIRED));
  }

  public static Stream<Arguments> invalidTimeIntervalProvider() {
    return Stream.of(Arguments.of((TimeInterval) null, ErrorCode.TIME_INTERVAL_REQUIRED));
  }
}
