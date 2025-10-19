package com.projetoExtensao.arenaMafia.application.operatingHours.usecase;

import java.util.UUID;

public interface DisableOperatingHoursUseCase {
  void execute(UUID hourId);
}
