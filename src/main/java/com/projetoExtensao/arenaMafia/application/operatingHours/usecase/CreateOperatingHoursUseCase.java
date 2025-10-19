package com.projetoExtensao.arenaMafia.application.operatingHours.usecase;

import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateOperatingHoursRequestDto;

public interface CreateOperatingHoursUseCase {
  OperatingHours execute(CreateOperatingHoursRequestDto request);
}
