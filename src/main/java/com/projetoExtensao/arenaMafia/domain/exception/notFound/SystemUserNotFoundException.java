package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class SystemUserNotFoundException extends NotFoundException {
  public SystemUserNotFoundException() {
    super(ErrorCode.SYSTEM_USER_NOT_FOUND);
  }
}
