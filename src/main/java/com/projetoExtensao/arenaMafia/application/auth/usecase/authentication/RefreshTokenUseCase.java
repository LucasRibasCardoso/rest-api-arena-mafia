package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication;

import com.projetoExtensao.arenaMafia.domain.dto.AuthResult;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;

public interface RefreshTokenUseCase {
  AuthResult execute(RefreshTokenVO refreshToken);
}
