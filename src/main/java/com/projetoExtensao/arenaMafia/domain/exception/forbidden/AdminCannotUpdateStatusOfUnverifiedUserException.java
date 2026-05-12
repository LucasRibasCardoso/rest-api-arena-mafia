package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class AdminCannotUpdateStatusOfUnverifiedUserException extends ForbiddenException {
  public AdminCannotUpdateStatusOfUnverifiedUserException(ErrorCode errorCode) {
    super(errorCode);
  }
}
