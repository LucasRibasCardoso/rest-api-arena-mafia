package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CreateCourtRequestDto(
    @NotBlank(message = "COURT_NAME_REQUIRED")
        @Size(min = 3, max = 100, message = "COURT_NAME_INVALID_LENGTH")
        String name,
    String description,
    @NotNull(message = "OFFSET_MINUTES_REQUIRED") OffsetMinutes offsetMinutes,
    @NotEmpty(message = "COURT_MODALITY_REQUIRED") Set<UUID> modalityIds) {}
