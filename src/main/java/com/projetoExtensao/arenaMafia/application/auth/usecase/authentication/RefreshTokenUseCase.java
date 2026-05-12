package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication;

import com.projetoExtensao.arenaMafia.application.auth.result.AuthResult;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;

public interface RefreshTokenUseCase {
  AuthResult execute(RefreshTokenVO refreshToken);
}
