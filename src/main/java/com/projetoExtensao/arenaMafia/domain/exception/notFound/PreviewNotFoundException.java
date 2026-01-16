package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PreviewNotFoundException extends NotFoundException {
  public PreviewNotFoundException() {
    super(ErrorCode.PREVIEW_NOT_FOUND);
  }
}
