package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidAgendaItemException extends BadRequestException {

  public InvalidAgendaItemException(ErrorCode errorCode) {
    super(errorCode);
  }
}

