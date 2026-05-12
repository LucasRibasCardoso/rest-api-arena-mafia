package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.response;

import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.BlockedTimeDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import java.util.List;
import java.util.UUID;

public record CourtDisablePreviewResponseDto(
    String previewKey,
    UUID courtId,
    String courtName,
    int usersAffectedCount,
    int blockedTimesAffectedCount,
    int reservationsAffectedCount,
    List<BlockedTimeDetailResponseDto> affectedBlockedTimes,
    List<ReservationDetailResponseDto> affectedReservations,
    List<ReservationDetailResponseDto> inProgressReservations) {}
