package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PriceRuleStatusConflictException extends ConflictException {
  public PriceRuleStatusConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}
