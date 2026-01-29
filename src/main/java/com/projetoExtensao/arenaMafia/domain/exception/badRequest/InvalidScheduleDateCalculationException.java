package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidScheduleDateCalculationException extends BadRequestException {
  public InvalidScheduleDateCalculationException(ErrorCode errorCode) {
    super(errorCode);
  }
}
