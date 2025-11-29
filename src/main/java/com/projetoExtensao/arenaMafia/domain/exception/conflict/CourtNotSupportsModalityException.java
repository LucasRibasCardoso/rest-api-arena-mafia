package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class CourtNotSupportsModalityException extends ConflictException {
  public CourtNotSupportsModalityException() {
    super(ErrorCode.COURT_NOT_SUPPORTS_MODALITY);
  }
}
