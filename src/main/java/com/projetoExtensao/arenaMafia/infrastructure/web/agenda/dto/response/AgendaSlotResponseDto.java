package com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.enums.AgendaSlotType;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgendaSlotResponseDto(
    UUID courtId,
    TimeIntervalDto timeInterval,
    AgendaSlotType slotType,
    Set<UUID> availableModalityIds,
    String description) {}
