package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class BatchCancellationFailedException extends ConflictException {
  public BatchCancellationFailedException() {
    super(ErrorCode.RESERVATION_CANCELLATION_IN_BATCH_FAILED);
  }
}
