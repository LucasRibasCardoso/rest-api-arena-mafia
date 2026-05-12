package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class DefaultPriceRuleNotFoundException extends NotFoundException {
  public DefaultPriceRuleNotFoundException() {
    super(ErrorCode.PRICE_RULE_DEFAULT_NOT_FOUND);
  }
}
