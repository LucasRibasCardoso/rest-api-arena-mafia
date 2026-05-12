package com.projetoExtensao.arenaMafia.application.schedule.result;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ScheduleEntryDetail;
import java.util.List;

public record ScheduleEntriesEnrichedResult(
    List<ScheduleEntryDetail> allEnrichedEntries,
    List<ReservationDetail> enrichedReservations,
    List<BlockedTimeDetail> enrichedBlockedTimes) {}
