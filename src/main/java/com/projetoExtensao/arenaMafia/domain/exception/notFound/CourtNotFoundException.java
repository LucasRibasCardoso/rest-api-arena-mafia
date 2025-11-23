package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class CourtNotFoundException extends NotFoundException {
  public CourtNotFoundException() {
    super(ErrorCode.COURT_NOT_FOUND);
  }

  public CourtNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }
}
