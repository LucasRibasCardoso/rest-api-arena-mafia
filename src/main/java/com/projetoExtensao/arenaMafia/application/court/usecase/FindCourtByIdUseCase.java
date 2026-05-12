package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.application.court.aggregate.CourtWithModalities;
import java.util.UUID;

public interface FindCourtByIdUseCase {
  CourtWithModalities execute(UUID courtId);
}
