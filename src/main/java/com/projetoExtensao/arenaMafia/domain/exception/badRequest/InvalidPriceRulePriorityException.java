package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidPriceRulePriorityException extends BadRequestException {
  public InvalidPriceRulePriorityException(ErrorCode errorCode) {
    super(errorCode);
  }
}
