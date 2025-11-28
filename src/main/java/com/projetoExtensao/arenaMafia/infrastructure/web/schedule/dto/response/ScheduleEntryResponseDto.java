package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Interface base para DTOs de resposta de entradas no calendário de agendamentos. Define apenas os
 * campos comuns a todos os tipos de ScheduleEntry..
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ReservationScheduleResponseDto.class, name = "RESERVATION")
})
public sealed interface ScheduleEntryResponseDto permits ReservationScheduleResponseDto {

  UUID id();

  ScheduleEntryType type();

  UUID courtId();

  LocalDate date();

  TimeIntervalDto timeInterval();

  Instant createdAt();
}
