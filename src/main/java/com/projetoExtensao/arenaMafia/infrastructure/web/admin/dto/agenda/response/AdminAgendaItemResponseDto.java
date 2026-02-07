package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.agenda.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AdminAvailableItemResponseDto.class, name = "AVAILABLE_SLOT"),
  @JsonSubTypes.Type(value = AdminScheduleDetailResponseDto.class, name = "SCHEDULE_DETAIL")
})
public sealed interface AdminAgendaItemResponseDto
    permits AdminAvailableItemResponseDto, AdminScheduleDetailResponseDto {

  TimeIntervalDto timeInterval();

}
