package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidPriceRuleNameFormatException extends BadRequestException {
  public InvalidPriceRuleNameFormatException(ErrorCode errorCode) {
    super(errorCode);
  }
}
