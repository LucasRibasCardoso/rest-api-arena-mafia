package com.projetoExtensao.arenaMafia.application.operatingHours.usecase;

import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import java.util.UUID;

public interface FindByIdOperatingHoursUseCase {
  OperatingHours execute(UUID hourId);
}
