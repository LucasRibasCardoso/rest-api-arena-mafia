package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class AdminCannotUpdateOwnStatusException extends ForbiddenException {
  public AdminCannotUpdateOwnStatusException(ErrorCode errorCode) {
    super(errorCode);
  }
}
