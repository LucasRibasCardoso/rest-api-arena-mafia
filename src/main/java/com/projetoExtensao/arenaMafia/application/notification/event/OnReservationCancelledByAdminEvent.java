package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;

public record OnReservationCancelledByAdminEvent(
    Reservation reservation, String username, String userPhone, String adminReason) {}
