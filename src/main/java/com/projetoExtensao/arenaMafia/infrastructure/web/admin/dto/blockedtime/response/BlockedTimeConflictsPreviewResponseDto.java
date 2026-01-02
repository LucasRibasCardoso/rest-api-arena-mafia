package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import java.util.List;

public record BlockedTimeConflictsPreviewResponseDto(
    String previewKey,
    Integer usersAffected,
    Integer blockedTimesAffected,
    Integer reservationsAffected,
    List<BlockedTimeDetail> conflictingBlockedTimes,
    List<ReservationDetail> conflictingReservations,
    List<ReservationDetail> inProgressReservations) {}
