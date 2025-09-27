package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidDateRangeException extends BadRequestException {
  public InvalidDateRangeException() {
    super(ErrorCode.START_DATE_AFTER_END_DATE);
  }
}
