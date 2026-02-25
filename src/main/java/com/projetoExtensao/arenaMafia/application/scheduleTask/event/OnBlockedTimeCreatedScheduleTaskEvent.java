package com.projetoExtensao.arenaMafia.application.scheduleTask.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;

public record OnBlockedTimeCreatedScheduleTaskEvent(BlockedTime blockedTime) {}
