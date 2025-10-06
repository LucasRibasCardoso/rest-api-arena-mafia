package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class CourtOffsetRequiredException extends BadRequestException {

  public CourtOffsetRequiredException() {
    super(ErrorCode.OFFSET_MINUTES_REQUIRED);
  }
}
