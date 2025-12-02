package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;

public record OnReservationCancelledEvent(
    Reservation reservation, String username, String userPhone) {}
