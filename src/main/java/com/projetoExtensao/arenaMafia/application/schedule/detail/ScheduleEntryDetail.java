package com.projetoExtensao.arenaMafia.application.schedule.detail;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.time.LocalDate;
import java.util.UUID;

public sealed interface ScheduleEntryDetail permits ReservationDetail, BlockedTimeDetail {

  UUID id();

  UUID courtId();

  String courtName();

  LocalDate date();

  TimeInterval timeInterval();
}
