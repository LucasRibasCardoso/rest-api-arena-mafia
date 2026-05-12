package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ReservationPastDateException extends BadRequestException {

  public ReservationPastDateException() {
    super(ErrorCode.RESERVATION_PAST_DATE_NOT_ALLOWED);
  }
}
