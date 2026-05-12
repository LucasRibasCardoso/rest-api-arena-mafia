package com.projetoExtensao.arenaMafia.application.court.preview;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import java.util.List;
import java.util.UUID;

public record CourtDisablePreview(
    String previewKey,
    UUID courtId,
    String courtName,
    int usersAffectedCount,
    int blockedTimesAffectedCount,
    int reservationsAffectedCount,
    List<BlockedTimeDetail> affectedBlockedTimes,
    List<ReservationDetail> affectedReservations,
    List<ReservationDetail> inProgressReservations) {}
