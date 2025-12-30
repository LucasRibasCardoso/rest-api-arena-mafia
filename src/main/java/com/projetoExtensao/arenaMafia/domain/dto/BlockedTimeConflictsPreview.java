package com.projetoExtensao.arenaMafia.domain.dto;

import java.util.List;

public record BlockedTimeConflictsPreview(
    String previewKey,
    int blockedTimesAffected,
    int usersAffected,
    int reservationsAffected,
    List<BlockedTimeDetail> conflictingBlockedTimes,
    List<ReservationDetail> conflictingReservations) {}
