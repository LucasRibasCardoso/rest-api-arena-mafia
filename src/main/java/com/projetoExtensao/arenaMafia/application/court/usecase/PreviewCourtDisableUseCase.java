package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.application.court.preview.CourtDisablePreview;
import java.util.UUID;

public interface PreviewCourtDisableUseCase {
  CourtDisablePreview execute(UUID courtId, UUID adminId);
}
