package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;

public record OnReservationCreatedEvent(
    String username, String userPhone, ScheduleEntry scheduleEntry) {}
