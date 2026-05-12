package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.modality.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateModalityRequestDto(
    @NotBlank(message = "MODALITY_NAME_REQUIRED")
        @Size(min = 3, max = 100, message = "MODALITY_NAME_INVALID_LENGTH")
        String name) {}
