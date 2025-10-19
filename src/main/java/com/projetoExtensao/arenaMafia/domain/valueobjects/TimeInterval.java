package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOffsetMinutesException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import java.time.LocalTime;

public record TimeInterval(
    @JsonProperty("openTime") LocalTime openTime, @JsonProperty("closeTime") LocalTime closeTime) {

  public TimeInterval {
    if (openTime == null || closeTime == null) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_REQUIRED);
    }
    if (openTime.isAfter(closeTime) || openTime.equals(closeTime)) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_OPEN_AFTER_CLOSE);
    }
    try {
      OffsetMinutes.fromValue(openTime.getMinute());
      OffsetMinutes.fromValue(closeTime.getMinute());
    } catch (InvalidOffsetMinutesException e) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_INVALID_MINUTES);
    }
  }

  public void validateNoOverlap(TimeInterval other) {
    if (other == null) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_REQUIRED);
    }

    boolean startsBeforeOtherEnds = openTime.isBefore(other.closeTime);
    boolean endsAfterOtherStarts = closeTime.isAfter(other.openTime);

    if (startsBeforeOtherEnds && endsAfterOtherStarts) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_OVERLAP);
    }
  }
}
