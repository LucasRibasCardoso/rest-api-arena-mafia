package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.response;

import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminCourtResponseDto(
    UUID id,
    String name,
    String description,
    int offsetMinutes,
    boolean isActive,
    List<ModalityResponseDto> modalities,
    Instant createdAt) {

  public static AdminCourtResponseDto fromDomain(
      Court domain, List<ModalityResponseDto> modalities) {

    return new AdminCourtResponseDto(
        domain.getId(),
        domain.getName(),
        domain.getDescription(),
        domain.getOffsetMinutes().getValue(),
        domain.isActive(),
        modalities,
        domain.getCreatedAt());
  }
}
