package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class BlockedTimeConflictException extends ConflictException {
  public BlockedTimeConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}

