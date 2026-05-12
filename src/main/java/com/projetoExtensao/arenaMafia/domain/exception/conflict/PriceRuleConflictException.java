package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PriceRuleConflictException extends ConflictException {

  public PriceRuleConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}
