package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidBlockDateException extends BadRequestException {
  public InvalidBlockDateException(ErrorCode errorCode) {
    super(errorCode);
  }
}
