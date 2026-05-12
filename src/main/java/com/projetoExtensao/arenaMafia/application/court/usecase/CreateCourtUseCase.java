package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.application.court.aggregate.CourtWithModalities;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.CreateCourtRequestDto;

public interface CreateCourtUseCase {
  CourtWithModalities execute(CreateCourtRequestDto request);
}
