package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;

import java.util.List;
import java.util.Set;

public record OnRecurringReservationCreatedByAdminEvent(
        String username,
        String userPhone,
        Set<DayOfWeek> daysOfWeek,
        List<Reservation> reservations) {}
