package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateCourtRequestDto;

public interface CreateCourtUseCase {
  Court execute(CreateCourtRequestDto request);
}
