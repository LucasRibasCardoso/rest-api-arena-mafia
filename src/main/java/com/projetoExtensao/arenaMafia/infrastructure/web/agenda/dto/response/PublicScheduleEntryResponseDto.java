package com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response;

import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;

public record PublicScheduleEntryResponseDto(
    TimeIntervalDto timeInterval,
    ScheduleEntryType entryType,
    String description // Ex: "Reservado" ou "Manutenção"
) implements PublicAgendaItemResponseDto {}
