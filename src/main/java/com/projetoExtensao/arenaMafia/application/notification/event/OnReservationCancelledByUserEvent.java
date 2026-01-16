package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;

public record OnReservationCancelledByUserEvent(
    Reservation reservation, String username, String userPhone) {}
