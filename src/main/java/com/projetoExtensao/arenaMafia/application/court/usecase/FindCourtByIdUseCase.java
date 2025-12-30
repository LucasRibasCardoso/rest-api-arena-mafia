package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.domain.dto.CourtWithModalitiesResult;
import java.util.UUID;

public interface FindCourtByIdUseCase {
  CourtWithModalitiesResult execute(UUID courtId);
}
