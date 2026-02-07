package com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = PublicAvailableItemResponseDto.class, name = "AVAILABLE_SLOT"),
  @JsonSubTypes.Type(value = PublicScheduleEntryResponseDto.class, name = "SCHEDULE_ENTRY")
})
public sealed interface PublicAgendaItemResponseDto
    permits PublicAvailableItemResponseDto, PublicScheduleEntryResponseDto {

  TimeIntervalDto timeInterval();
}
