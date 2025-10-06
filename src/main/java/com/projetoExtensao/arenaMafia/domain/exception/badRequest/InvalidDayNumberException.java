package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidDayNumberException extends BadRequestException {

  public InvalidDayNumberException() {
    super(ErrorCode.INVALID_DAY_NUMBER);
  }
}
