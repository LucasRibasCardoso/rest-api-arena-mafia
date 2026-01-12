package com.projetoExtensao.arenaMafia.application.operatingHours.preview;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;

import java.util.List;
import java.util.UUID;

public record OperatingHoursDisablePreview(
    String previewKey,
    UUID operatingHoursId,
    int usersAffectedCount,
    int blockedTimesAffectedCount,
    int reservationsAffectedCount,
    List<BlockedTimeDetail> affectedBlockedTimes,
    List<ReservationDetail> affectedReservations,
    List<ReservationDetail> inProgressReservations) {}
