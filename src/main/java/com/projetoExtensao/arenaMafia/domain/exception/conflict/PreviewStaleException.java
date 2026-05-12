package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PreviewStaleException extends ConflictException {
  public PreviewStaleException() {
    super(ErrorCode.PREVIEW_DATA_STALE);
  }
}
