package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ReservationStatusConflictException extends ConflictException {

  public ReservationStatusConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}
