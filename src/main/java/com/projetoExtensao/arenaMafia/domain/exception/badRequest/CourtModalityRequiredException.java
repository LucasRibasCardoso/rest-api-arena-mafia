package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class CourtModalityRequiredException extends BadRequestException {

  public CourtModalityRequiredException() {
    super(ErrorCode.COURT_MODALITY_REQUIRED);
  }
}
