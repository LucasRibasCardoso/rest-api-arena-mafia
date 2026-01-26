package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.agenda.response;

import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record AdminAvailableItemResponseDto(
    UUID courtId,
    String courtName,
    TimeIntervalDto timeInterval,
    Set<UUID> availableModalityIds,
    BigDecimal price)
    implements AdminAgendaItemResponseDto {}
