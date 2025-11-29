package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidReservationException extends BadRequestException {
  public InvalidReservationException(ErrorCode errorCode) {
    super(errorCode);
  }
}
