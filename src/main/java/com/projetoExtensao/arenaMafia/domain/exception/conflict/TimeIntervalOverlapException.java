package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class TimeIntervalOverlapException extends ConflictException {
  public TimeIntervalOverlapException(ErrorCode errorCode) {
    super(errorCode);
  }
}
