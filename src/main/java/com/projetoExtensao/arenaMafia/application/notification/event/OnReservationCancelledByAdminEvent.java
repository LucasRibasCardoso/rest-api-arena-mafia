package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;

public record OnReservationCancelledByAdminEvent(
    String username, String userPhone, String adminReason, Reservation reservation) {}
