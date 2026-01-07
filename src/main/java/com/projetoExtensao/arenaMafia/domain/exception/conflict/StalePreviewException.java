package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class StalePreviewException extends ConflictException {
  public StalePreviewException() {
    super(ErrorCode.BLOCKED_TIME_PREVIEW_STALE);
  }
}

