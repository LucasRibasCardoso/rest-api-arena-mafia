package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidModalityNameFormatException extends BadRequestException {

  public InvalidModalityNameFormatException(ErrorCode errorCode) {
    super(errorCode);
  }
}
