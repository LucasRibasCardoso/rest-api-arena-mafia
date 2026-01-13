package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleNormal.ReservationResponseDto;
import org.springframework.stereotype.Component;

@Component
public class ReservationResponseMapper implements ScheduleEntryMapperStrategy<Reservation, ReservationDetail> {

  @Override
  public Class<Reservation> getSupportedType() {
    return Reservation.class;
  }

  @Override
  public Class<ReservationDetail> getSupportedDetailType() {
    return ReservationDetail.class;
  }

  @Override
  public ReservationResponseDto toDto(Reservation reservation) {
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

  @Override
  public ReservationDetailResponseDto toDetailDto(ReservationDetail detail) {
    TimeIntervalDto timeIntervalDto =
        new TimeIntervalDto(detail.timeInterval().startTime(), detail.timeInterval().endTime());

    return new ReservationDetailResponseDto(
        detail.reservationId(),
        detail.userId(),
        detail.courtId(),
        detail.username(),
        detail.userPhone(),
        detail.courtName(),
        detail.date(),
        timeIntervalDto,
        detail.modalityName(),
        detail.price(),
        detail.status(),
        detail.recurringReservationId());
  }
}
