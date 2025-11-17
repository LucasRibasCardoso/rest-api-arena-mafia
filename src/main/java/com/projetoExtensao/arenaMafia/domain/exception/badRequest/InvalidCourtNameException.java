package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidCourtNameException extends BadRequestException {

  public InvalidCourtNameException(ErrorCode errorCode) {
    super(errorCode);
  }
}
