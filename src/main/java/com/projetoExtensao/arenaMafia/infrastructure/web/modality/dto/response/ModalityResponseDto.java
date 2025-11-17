package com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ModalityResponseDto(UUID id, String name, boolean isActive, Instant createdAt) {}
