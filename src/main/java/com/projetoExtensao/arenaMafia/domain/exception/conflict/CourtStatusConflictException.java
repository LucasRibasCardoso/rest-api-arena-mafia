package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class CourtStatusConflictException extends ConflictException {
  public CourtStatusConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}
