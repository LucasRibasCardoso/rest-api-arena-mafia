package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response;

import com.projetoExtensao.arenaMafia.domain.dto.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.domain.dto.ReservationDetail;
import java.util.List;

public record BlockedTimeConflictsPreviewResponseDto(
    String previewKey,
    Integer usersAffected,
    Integer blockedTimesAffected,
    Integer reservationsAffected,
    List<BlockedTimeDetail> conflictingBlockedTimes,
    List<ReservationDetail> conflictingReservations) {}

