package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class AccountStatusConflictException extends ConflictException {
  public AccountStatusConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}
