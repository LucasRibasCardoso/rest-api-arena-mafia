package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ModalityInUseException extends ConflictException {
  public ModalityInUseException() {
    super(ErrorCode.MODALITY_IN_USE);
  }
}
