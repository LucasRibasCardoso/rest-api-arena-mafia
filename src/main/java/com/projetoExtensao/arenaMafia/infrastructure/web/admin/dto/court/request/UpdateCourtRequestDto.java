package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record UpdateCourtRequestDto(
    @Size(min = 3, max = 100, message = "COURT_NAME_INVALID_LENGTH") String name,
    String description,
    OffsetMinutes offsetMinutes,
    @Size(min = 1, message = "COURT_MODALITY_REQUIRED") Set<UUID> modalityIds) {}
