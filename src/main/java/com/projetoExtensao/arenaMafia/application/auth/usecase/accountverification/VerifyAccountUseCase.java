package com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification;

import com.projetoExtensao.arenaMafia.domain.dto.AuthResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;

public interface VerifyAccountUseCase {
  AuthResult execute(ValidateOtpRequestDto request);
}
