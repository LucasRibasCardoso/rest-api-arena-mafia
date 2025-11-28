package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class UnsupportedScheduleEntryTypeException extends BadRequestException {
  public UnsupportedScheduleEntryTypeException() {

    super(ErrorCode.UNSUPPORTED_SCHEDULE_ENTRY_TYPE);
  }
}
