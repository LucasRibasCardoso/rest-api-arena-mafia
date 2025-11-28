package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class CourtNotSupportsModalityException extends BadRequestException {
  public CourtNotSupportsModalityException() {
    super(ErrorCode.COURT_NOT_SUPPORTS_MODALITY);
  }
}
