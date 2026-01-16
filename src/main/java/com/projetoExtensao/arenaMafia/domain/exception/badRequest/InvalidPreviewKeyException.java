package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidPreviewKeyException extends BadRequestException {
  public InvalidPreviewKeyException() {
    super(ErrorCode.PREVIEW_KEY_INVALID);
  }
}
