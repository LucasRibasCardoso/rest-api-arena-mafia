package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.application.court.dto.CourtWithModalitiesResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateCourtRequestDto;

public interface CreateCourtUseCase {
  CourtWithModalitiesResult execute(CreateCourtRequestDto request);
}
