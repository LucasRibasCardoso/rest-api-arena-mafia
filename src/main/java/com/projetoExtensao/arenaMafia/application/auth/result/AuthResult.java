package com.projetoExtensao.arenaMafia.application.auth.result;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;

public record AuthResult(User user, String accessToken, RefreshTokenVO refreshToken) {}
