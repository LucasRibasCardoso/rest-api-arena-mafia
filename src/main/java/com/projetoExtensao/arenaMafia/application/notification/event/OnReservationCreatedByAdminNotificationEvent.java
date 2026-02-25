package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;

public record OnReservationCreatedByAdminNotificationEvent(
    String username, String userPhone, Reservation reservation) {}
