package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateReservationRequestDto(
    @NotNull(message = "RESERVATION_MODALITY_ID_REQUIRED") UUID modalityId,
    @NotNull(message = "RESERVATION_COURT_ID_REQUIRED") UUID courtId,
    @NotNull(message = "RESERVATION_DATE_REQUIRED") LocalDate date,
    @NotNull(message = "RESERVATION_TIME_INTERVAL_REQUIRED") @Valid TimeInterval timeInterval) {}
