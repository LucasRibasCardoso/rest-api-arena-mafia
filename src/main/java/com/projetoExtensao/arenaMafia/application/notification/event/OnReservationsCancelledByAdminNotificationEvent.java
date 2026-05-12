package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.List;

public record OnReservationsCancelledByAdminNotificationEvent(
    String username, String userPhone, String adminReason, List<Reservation> reservations) {}
