package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.BlockedTimeEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ReservationEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ScheduleEntryEntity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
    componentModel = "spring",
    uses = {ReservationMapper.class})
public abstract class ScheduleEntryMapper {

  @Autowired protected BlockedTimeMapper blockedTimeMapper;
  @Autowired protected ReservationMapper reservationMapper;

  public ScheduleEntry toDomain(ScheduleEntryEntity entity) {
    if (entity == null) {
      return null;
    }

    // Polimorfismo: delega para o mapper específico baseado no tipo da entidade
    if (entity instanceof ReservationEntity reservationEntity) {
      return reservationMapper.toDomain(reservationEntity);
    }

    if (entity instanceof BlockedTimeEntity blockedTimeEntity) {
      return blockedTimeMapper.toDomain(blockedTimeEntity);
    }

    throw new IllegalArgumentException(
        "Tipo de ScheduleEntryEntity não suportado: " + entity.getClass().getName());
  }

  public ScheduleEntryEntity toEntity(ScheduleEntry domain) {
    if (domain == null) {
      return null;
    }

    if (domain instanceof Reservation reservation) {
      return reservationMapper.toEntity(reservation);
    }

    if (domain instanceof BlockedTime blockedTime) {
      return blockedTimeMapper.toEntity(blockedTime);
    }

    throw new IllegalArgumentException(
        "Tipo de ScheduleEntry não suportado: " + domain.getClass().getName());
  }
}
