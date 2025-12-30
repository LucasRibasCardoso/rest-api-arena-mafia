package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.ReservationResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.ScheduleEntryResponseDto;
import org.springframework.stereotype.Component;

@Component
public class ReservationResponseMapper implements ScheduleEntryMapperStrategy<Reservation> {

  @Override
  public Class<Reservation> getSupportedType() {
    return Reservation.class;
  }

  @Override
  public ScheduleEntryResponseDto toDto(Reservation reservation) {
    TimeIntervalDto timeIntervalDto =
        new TimeIntervalDto(
            reservation.getDateTimeSlot().timeInterval().startTime(),
            reservation.getDateTimeSlot().timeInterval().endTime());

    return new ReservationResponseDto(
        reservation.getId(),
        ScheduleEntryType.RESERVATION,
        reservation.getCourtId(),
        reservation.getDateTimeSlot().date(),
        timeIntervalDto,
        reservation.getCreatedAt(),
        reservation.getUserId(),
        reservation.getModalityId(),
        reservation.getScheduledByAdminId(),
        reservation.getPrice(),
        reservation.getStatus(),
        reservation.getRecurringReservationId());
  }
}
