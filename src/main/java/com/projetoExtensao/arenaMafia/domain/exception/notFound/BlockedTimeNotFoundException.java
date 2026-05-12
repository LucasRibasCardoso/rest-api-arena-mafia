package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class BlockedTimeNotFoundException extends NotFoundException {
  public BlockedTimeNotFoundException() {
    super(ErrorCode.BLOCKED_TIME_NOT_FOUND);
  }
}
