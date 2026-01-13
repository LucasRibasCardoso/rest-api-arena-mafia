package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OperatingHoursDisableConfirmRequestDto(
    @NotBlank(message = "PREVIEW_KEY_REQUIRED") String previewKey,
    @NotBlank(message = "OPERATING_HOURS_DISABLE_DESCRIPTION_REQUIRED")
        @Size(min = 3, max = 500, message = "OPERATING_HOURS_DISABLE_DESCRIPTION_INVALID_LENGTH")
        String description) {}
