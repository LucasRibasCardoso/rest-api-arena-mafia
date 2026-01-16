package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidBlockedTimeException extends BadRequestException {
  public InvalidBlockedTimeException(ErrorCode errorCode) {
    super(errorCode);
  }
}
