package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response;

import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.BlockedTimeDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import java.util.List;

public record BlockedTimeConflictsPreviewResponseDto(
    String previewKey,
    int usersAffected,
    int blockedTimesAffected,
    int reservationsAffected,
    List<BlockedTimeDetailResponseDto> conflictingBlockedTimes,
    List<ReservationDetailResponseDto> conflictingReservations,
    List<ReservationDetailResponseDto> inProgressReservations) {}
