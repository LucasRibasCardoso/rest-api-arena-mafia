package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.application.court.dto.CourtWithModalitiesResult;
import java.util.List;

public interface FindAllCourtUseCase {
  List<CourtWithModalitiesResult> execute(Boolean isActive);
}
