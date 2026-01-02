package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.application.court.aggregate.CourtWithModalities;
import java.util.List;

public interface FindAllCourtUseCase {
  List<CourtWithModalities> execute(Boolean isActive);
}
