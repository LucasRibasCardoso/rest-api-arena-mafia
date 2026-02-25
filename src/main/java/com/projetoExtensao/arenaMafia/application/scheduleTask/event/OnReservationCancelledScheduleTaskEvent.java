package com.projetoExtensao.arenaMafia.application.scheduleTask.event;

import java.util.UUID;

public record OnReservationCancelledScheduleTaskEvent(UUID reservationId) {}
