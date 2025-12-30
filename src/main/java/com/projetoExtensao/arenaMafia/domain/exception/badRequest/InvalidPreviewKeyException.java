package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidPreviewKeyException extends BadRequestException {
  public InvalidPreviewKeyException() {
    super(ErrorCode.BLOCKED_TIME_PREVIEW_KEY_INVALID);
  }
}

