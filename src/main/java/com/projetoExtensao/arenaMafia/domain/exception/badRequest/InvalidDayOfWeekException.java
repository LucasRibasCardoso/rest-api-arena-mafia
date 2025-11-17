package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidDayOfWeekException extends BadRequestException {

  public InvalidDayOfWeekException(ErrorCode errorCode) {
    super(errorCode);
  }
}
