package com.projetoExtensao.arenaMafia.application.schedule.preview;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import java.util.List;

public record BlockedTimeConflictsPreview(
    String previewKey,
    int usersAffected,
    int blockedTimesAffected,
    int reservationsAffected,
    List<BlockedTimeDetail> conflictingBlockedTimes,
    List<ReservationDetail> conflictingReservations,
    List<ReservationDetail> inProgressReservations,
    BlockedTimeConflictsPreviewRequestDto request) {

  public BlockedTimeConflictsPreview createDefaultWithRequest(BlockedTimeConflictsPreviewRequestDto request) {
    return new BlockedTimeConflictsPreview(null, 0, 0, 0, List.of(), List.of(), List.of(), request);
  }
}
