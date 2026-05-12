package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class AdminCannotUpdateRoleOfUnverifiedUserException extends ForbiddenException {
  public AdminCannotUpdateRoleOfUnverifiedUserException(ErrorCode errorCode) {
    super(errorCode);
  }
}
