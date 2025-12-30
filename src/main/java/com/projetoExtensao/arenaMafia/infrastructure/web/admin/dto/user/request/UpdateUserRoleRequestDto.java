package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequestDto(@NotNull(message = "ROLE_REQUIRED") RoleEnum role) {}
