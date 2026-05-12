package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BlockedTimeUpdateRequestDto(
    @NotBlank(message = "BLOCKED_TIME_DESCRIPTION_REQUIRED")
        @Size(min = 3, max = 500, message = "BLOCKED_TIME_DESCRIPTION_INVALID_LENGTH")
        String description,
    @NotNull(message = "BLOCKED_TIME_UPDATE_RECURRING_FLAG_REQUIRED") Boolean updateAllRecurring) {}
