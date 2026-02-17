package com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response;

import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record PublicAvailableItemResponseDto(
    TimeIntervalDto timeInterval, Set<UUID> availableModalityIds, BigDecimal price)
    implements PublicAgendaItemResponseDto {}
