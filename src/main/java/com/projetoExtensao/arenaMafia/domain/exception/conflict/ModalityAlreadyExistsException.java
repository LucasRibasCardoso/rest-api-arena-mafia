package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ModalityAlreadyExistsException extends ConflictException {
  public ModalityAlreadyExistsException() {
    super(ErrorCode.MODALITY_ALREADY_EXISTS);
  }
}
