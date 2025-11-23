package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidScheduleEntryException extends BadRequestException {
  public InvalidScheduleEntryException(ErrorCode errorCode) {
    super(errorCode);
  }
}
