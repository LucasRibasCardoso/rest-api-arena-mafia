package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class OperatingHoursNotFoundException extends NotFoundException {

  public OperatingHoursNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }
}
