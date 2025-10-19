package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidDayNumberException extends BadRequestException {

  public InvalidDayNumberException() {
    super(ErrorCode.DAY_OF_WEEK_INVALID);
  }
}
