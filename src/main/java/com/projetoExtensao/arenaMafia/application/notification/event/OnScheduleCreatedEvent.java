package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;

public record OnScheduleCreatedEvent(String username, String userPhone, ScheduleEntry scheduleEntry) {}
