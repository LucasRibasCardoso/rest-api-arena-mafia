package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ScheduleNotFoundException extends NotFoundException {
  public ScheduleNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }
}
