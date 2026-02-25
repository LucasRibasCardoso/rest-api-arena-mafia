package com.projetoExtensao.arenaMafia.application.scheduleTask.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;

public record OnReservationCreatedScheduleTaskEvent(Reservation reservation) {}
