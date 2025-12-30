package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.domain.dto.CourtWithModalitiesResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.CreateCourtRequestDto;

public interface CreateCourtUseCase {
  CourtWithModalitiesResult execute(CreateCourtRequestDto request);
}
