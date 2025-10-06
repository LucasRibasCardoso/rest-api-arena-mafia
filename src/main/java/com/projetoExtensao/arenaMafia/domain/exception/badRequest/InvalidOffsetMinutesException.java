package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidOffsetMinutesException extends BadRequestException {

  public InvalidOffsetMinutesException() {
    super(ErrorCode.INVALID_OFFSET_MINUTES);
  }
}
