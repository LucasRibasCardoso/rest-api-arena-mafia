package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidTimeIntervalException extends BadRequestException {

  public InvalidTimeIntervalException(ErrorCode errorCode) {
    super(errorCode);
  }
}
