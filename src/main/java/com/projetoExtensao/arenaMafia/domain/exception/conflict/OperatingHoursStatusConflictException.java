package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class OperatingHoursStatusConflictException extends ConflictException {
  public OperatingHoursStatusConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}
