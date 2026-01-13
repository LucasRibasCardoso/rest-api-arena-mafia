package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.BlockedTimeDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleNormal.BlockedTimeResponseDto;
import org.springframework.stereotype.Component;

@Component
public class BlockedTimeResponseMapper implements ScheduleEntryMapperStrategy<BlockedTime, BlockedTimeDetail> {

  @Override
  public Class<BlockedTime> getSupportedType() {
    return BlockedTime.class;
  }

  @Override
  public Class<BlockedTimeDetail> getSupportedDetailType() {
    return BlockedTimeDetail.class;
  }

  @Override
  public BlockedTimeResponseDto toDto(BlockedTime blockedTime) {
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

  @Override
  public BlockedTimeDetailResponseDto toDetailDto(BlockedTimeDetail detail) {
    TimeIntervalDto timeIntervalDto =
        new TimeIntervalDto(
                detail.timeInterval().startTime(),
                detail.timeInterval().endTime());

    return new BlockedTimeDetailResponseDto(
        detail.blockedTimeId(),
        detail.courtId(),
        detail.courtName(),
        detail.date(),
        timeIntervalDto,
        detail.description(),
        detail.isFullDay(),
        detail.recurringBlockedTimeId());
  }
}
