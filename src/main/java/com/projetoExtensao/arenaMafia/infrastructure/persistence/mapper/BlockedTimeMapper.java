package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.BlockedTimeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public abstract class BlockedTimeMapper {

  public abstract BlockedTimeEntity toEntity(BlockedTime blockedTime);

  public abstract BlockedTime toDomain(BlockedTimeEntity blockedTimeEntity);

  @ObjectFactory
  public BlockedTime createBlockedTime(BlockedTimeEntity blockedTimeEntity) {
    return BlockedTime.reconstitute(
        blockedTimeEntity.getId(),
        blockedTimeEntity.getCourtId(),
        blockedTimeEntity.getDateTimeSlot(),
        blockedTimeEntity.getDescription(),
        blockedTimeEntity.getBlockedByAdminId(),
        blockedTimeEntity.isFullDay(),
        blockedTimeEntity.getRecurringBlockedTimeId(),
        blockedTimeEntity.getCreatedAt());
  }
}
