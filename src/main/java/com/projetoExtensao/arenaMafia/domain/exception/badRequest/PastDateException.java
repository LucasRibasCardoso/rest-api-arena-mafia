package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PastDateException extends BadRequestException {

  public PastDateException() {
    super(ErrorCode.PAST_DATE_NOT_ALLOWED);
  }
}

