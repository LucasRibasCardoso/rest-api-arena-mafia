package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidPriceException extends BadRequestException {
  public InvalidPriceException(ErrorCode errorCode) {
    super(errorCode);
  }
}
