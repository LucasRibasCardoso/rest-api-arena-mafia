package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class AdminCannotUpdateOwnRoleException extends ForbiddenException {
  public AdminCannotUpdateOwnRoleException(ErrorCode errorCode) {
    super(errorCode);
  }
}
