package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class OperatingHoursNotFoundException extends NotFoundException {

  public OperatingHoursNotFoundException() {
    super(ErrorCode.OPERATING_HOURS_NOT_FOUND);
  }

  public OperatingHoursNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }
}
