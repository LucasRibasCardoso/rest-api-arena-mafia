package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.application.court.dto.CourtWithModalitiesResult;
import java.util.UUID;

public interface FindCourtByIdUseCase {
  CourtWithModalitiesResult execute(UUID courtId);
}
