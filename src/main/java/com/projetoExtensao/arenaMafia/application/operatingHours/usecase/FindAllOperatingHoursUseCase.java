package com.projetoExtensao.arenaMafia.application.operatingHours.usecase;

import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import java.util.List;

public interface FindAllOperatingHoursUseCase {
  List<OperatingHours> execute(Boolean isActive);
}
