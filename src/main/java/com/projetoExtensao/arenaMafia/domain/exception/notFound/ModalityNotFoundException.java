package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ModalityNotFoundException extends NotFoundException {
  public ModalityNotFoundException() {
    super(ErrorCode.MODALITY_NOT_FOUND);
  }
}
