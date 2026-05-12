package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PriceRuleAlreadyExistsException extends ConflictException {
  public PriceRuleAlreadyExistsException() {
    super(ErrorCode.PRICE_RULE_ALREADY_EXISTS);
  }
}
