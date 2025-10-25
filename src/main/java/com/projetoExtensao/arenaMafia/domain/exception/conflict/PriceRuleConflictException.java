package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PriceRuleConflictException extends ConflictException {

  public PriceRuleConflictException() {
    super(ErrorCode.PRICE_RULE_OVERLAP);
  }
}


