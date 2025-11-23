package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDateTimeSlotException;
import java.time.LocalDate;

public record DateTimeSlot(LocalDate date, TimeInterval timeInterval) {

  public DateTimeSlot {
    if (date == null) {
      throw new InvalidDateTimeSlotException(ErrorCode.DATE_TIME_SLOT_DATE_REQUIRED);
    }

    if (timeInterval == null) {
      throw new InvalidDateTimeSlotException(ErrorCode.DATE_TIME_SLOT_TIME_INTERVAL_REQUIRED);
    }
  }
}
