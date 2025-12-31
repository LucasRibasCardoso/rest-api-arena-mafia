package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class CourtCannotBeDisabledException extends ConflictException {
  public CourtCannotBeDisabledException() {
    super(ErrorCode.COURT_CANNOT_BE_DISABLED_DUE_TO_FUTURE_RESERVATIONS);
  }
}
