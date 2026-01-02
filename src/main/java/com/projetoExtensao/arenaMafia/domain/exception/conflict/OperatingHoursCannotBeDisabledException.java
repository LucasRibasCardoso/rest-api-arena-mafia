package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class OperatingHoursCannotBeDisabledException extends ConflictException {
  public OperatingHoursCannotBeDisabledException() {
    super(ErrorCode.OPERATING_HOURS_CANNOT_BE_DISABLED_DUE_TO_RESERVATIONS);
  }
}
