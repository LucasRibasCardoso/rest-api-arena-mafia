package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime;

import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;

import java.util.UUID;

public interface PreviewBlockedTimeConflictsUseCase {
  BlockedTimeConflictsPreview execute(BlockedTimeConflictsPreviewRequestDto request, UUID adminId);
}
