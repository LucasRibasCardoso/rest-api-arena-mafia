package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidPreviewOwnershipException extends ForbiddenException {
  public InvalidPreviewOwnershipException() {
    super(ErrorCode.BLOCKED_TIME_PREVIEW_OWNERSHIP_INVALID);
  }
}

