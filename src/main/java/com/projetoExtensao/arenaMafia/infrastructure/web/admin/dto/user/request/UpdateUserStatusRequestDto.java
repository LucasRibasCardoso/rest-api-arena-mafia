package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequestDto(
    @NotNull(message = "ACCOUNT_STATUS_REQUIRED") AccountStatus status) {}
