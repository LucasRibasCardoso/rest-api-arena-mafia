package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ModalityStatusConflictException extends ConflictException {
    public ModalityStatusConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
}
