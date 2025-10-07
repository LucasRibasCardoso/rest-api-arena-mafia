package com.projetoExtensao.arenaMafia.application.court.usecase;

import java.util.UUID;

import com.projetoExtensao.arenaMafia.application.court.dto.CourtWithModalitiesResult;

public interface FindCourtByIdUseCase {
    CourtWithModalitiesResult execute (UUID courtId);
}
