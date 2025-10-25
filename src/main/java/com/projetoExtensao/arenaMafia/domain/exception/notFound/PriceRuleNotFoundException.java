package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PriceRuleNotFoundException extends NotFoundException {
  public PriceRuleNotFoundException() {
    super(ErrorCode.PRICE_RULE_NOT_FOUND);
  }
}
