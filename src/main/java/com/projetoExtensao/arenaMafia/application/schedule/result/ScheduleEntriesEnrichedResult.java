package com.projetoExtensao.arenaMafia.application.schedule.result;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ScheduleDetail;
import java.util.List;

public record ScheduleEntriesEnrichedResult(
    List<ScheduleDetail> allEnrichedEntries,
    List<ReservationDetail> enrichedReservations,
    List<BlockedTimeDetail> enrichedBlockedTimes) {}
