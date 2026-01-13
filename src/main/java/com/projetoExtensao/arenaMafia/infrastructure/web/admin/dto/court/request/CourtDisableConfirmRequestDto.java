package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourtDisableConfirmRequestDto(
    @NotBlank(message = "PREVIEW_KEY_REQUIRED") String previewKey,
    @NotBlank(message = "COURT_DISABLE_DESCRIPTION_REQUIRED")
        @Size(min = 3, max = 500, message = "COURT_DISABLE_DESCRIPTION_INVALID_LENGTH")
        String description) {}
