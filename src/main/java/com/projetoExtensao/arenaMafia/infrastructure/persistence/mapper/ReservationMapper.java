package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public abstract class ReservationMapper {

  public abstract ReservationEntity toEntity(Reservation reservation);

  public abstract Reservation toDomain(ReservationEntity entity);

  @ObjectFactory
  public Reservation createReservation(ReservationEntity entity) {
    return Reservation.reconstitute(
        entity.getId(),
        entity.getCourtId(),
        entity.getModalityId(),
        entity.getUserId(),
        entity.getScheduledByAdminId(),
        entity.getCancelledByAdminId(),
        entity.getPrice(),
        entity.getDateTimeSlot(),
        entity.getStatus(),
        entity.getRecurringReservationId(),
        entity.getCreatedAt());
  }

  public TimeIntervalDto toTimeIntervalDto(TimeInterval timeInterval) {
    if (timeInterval == null) {
      return null;
    }
    return new TimeIntervalDto(timeInterval.startTime(), timeInterval.endTime());
  }
}
