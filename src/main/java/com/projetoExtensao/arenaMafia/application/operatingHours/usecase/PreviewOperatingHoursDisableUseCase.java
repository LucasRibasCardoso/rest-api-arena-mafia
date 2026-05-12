package com.projetoExtensao.arenaMafia.application.operatingHours.usecase;

import com.projetoExtensao.arenaMafia.application.operatingHours.preview.OperatingHoursDisablePreview;
import java.util.UUID;

public interface PreviewOperatingHoursDisableUseCase {

  OperatingHoursDisablePreview execute(UUID adminId, UUID operatingHoursId);
}
