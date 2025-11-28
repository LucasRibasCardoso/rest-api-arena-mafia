package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ScheduleConflictException extends ConflictException {
  public ScheduleConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}
