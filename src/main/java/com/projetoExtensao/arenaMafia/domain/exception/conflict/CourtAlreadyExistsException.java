package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class CourtAlreadyExistsException extends ConflictException {

  public CourtAlreadyExistsException() {
    super(ErrorCode.COURT_ALREADY_EXISTS);
  }
}
