package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper;

import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.BlockedTimeResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.ScheduleEntryResponseDto;
import org.springframework.stereotype.Component;

@Component
public class BlockedTimeResponseMapper implements ScheduleEntryMapperStrategy<BlockedTime> {

  @Override
  public Class<BlockedTime> getSupportedType() {
    return BlockedTime.class;
  }

  @Override
  public ScheduleEntryResponseDto toDto(BlockedTime blockedTime) {
    TimeIntervalDto timeIntervalDto =
        new TimeIntervalDto(
            blockedTime.getDateTimeSlot().timeInterval().startTime(),
            blockedTime.getDateTimeSlot().timeInterval().endTime());

    return new BlockedTimeResponseDto(
        blockedTime.getId(),
        ScheduleEntryType.BLOCKED_TIME,
        blockedTime.getCourtId(),
        blockedTime.getDateTimeSlot().date(),
        timeIntervalDto,
        blockedTime.getCreatedAt(),
        blockedTime.getDescription(),
        blockedTime.isFullDay());
  }
}
