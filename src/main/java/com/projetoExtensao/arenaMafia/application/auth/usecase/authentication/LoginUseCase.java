package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication;

import com.projetoExtensao.arenaMafia.application.auth.result.AuthResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;

public interface LoginUseCase {
  AuthResult execute(LoginRequestDto loginRequestDto);
}
