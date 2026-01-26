package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.agenda.response;

import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ScheduleDetailResponseDto;

public record AdminScheduleDetailResponseDto(
    ScheduleDetailResponseDto detail) implements AdminAgendaItemResponseDto {

  @Override
  public TimeIntervalDto timeInterval() {
    return detail.timeInterval();
  }
}
