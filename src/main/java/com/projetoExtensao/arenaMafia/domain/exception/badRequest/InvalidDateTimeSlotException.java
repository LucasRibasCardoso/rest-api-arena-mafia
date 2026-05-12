package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidDateTimeSlotException extends BadRequestException {

  public InvalidDateTimeSlotException(ErrorCode errorCode) {
    super(errorCode);
  }
}
