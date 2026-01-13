package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ReservationAccessDeniedException extends ForbiddenException {
  public ReservationAccessDeniedException() {
    super(ErrorCode.RESERVATION_ACCESS_DENIED);
  }
}
